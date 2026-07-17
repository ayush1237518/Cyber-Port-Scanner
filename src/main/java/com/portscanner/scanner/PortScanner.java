package com.portscanner.scanner;

import com.portscanner.model.ScanConfig;
import com.portscanner.model.ScanResult;
import com.portscanner.model.ScanStatistics;
import com.portscanner.threading.ScanExecutorManager;
import com.portscanner.utils.AppLogger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core scanning engine. Resolves the target host, fans out one {@link ScanTask}
 * per port across a fixed-size thread pool, and reports live progress through
 * a {@link ScanProgressListener}.
 * <p>
 * A single {@code PortScanner} instance is intended to run exactly one scan.
 * Create a new instance for each scan job.
 */
public final class PortScanner {

    private final ScanConfig config;
    private final ScanProgressListener listener;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final List<ScanResult> results = Collections.synchronizedList(new ArrayList<>());

    private volatile ScanExecutorManager executorManager;
    private volatile long scanStartMillis;

    public PortScanner(ScanConfig config, ScanProgressListener listener) {
        this.config = config;
        this.listener = listener;
    }

    /**
     * Runs the scan synchronously on the calling thread (which should itself
     * be a background thread, e.g. a JavaFX {@code Task}). Internally, the
     * actual port probes are parallelized across {@code config.getThreadCount()} workers.
     *
     * @return final scan statistics, or {@code null} if the scan failed to start
     */
    public ScanStatistics runScan() {
        scanStartMillis = System.currentTimeMillis();
        InetAddress resolvedAddress;

        try {
            resolvedAddress = InetAddress.getByName(config.getTarget());
            AppLogger.info("Resolved target '%s' to %s".formatted(config.getTarget(), resolvedAddress.getHostAddress()));
        } catch (UnknownHostException ex) {
            AppLogger.error("DNS resolution failed for target '%s'".formatted(config.getTarget()), ex);
            listener.onScanFailed("Could not resolve host: " + config.getTarget());
            return null;
        }

        InetSocketAddress targetAddress = new InetSocketAddress(resolvedAddress, 0);
        executorManager = new ScanExecutorManager(config.getThreadCount());
        ExecutorService executor = executorManager.getExecutorService();

        int totalPorts = config.getTotalPorts();
        List<Future<ScanResult>> futures = new ArrayList<>(totalPorts);

        AppLogger.info("Starting scan of %s (ports %d-%d) with %d threads."
                .formatted(config.getTarget(), config.getStartPort(), config.getEndPort(), config.getThreadCount()));

        // Shared across every task in this scan so a host-unreachable condition
        // (which affects every port identically) is only logged once, not once per port.
        AtomicBoolean unreachableWarned = new AtomicBoolean(false);

        for (int port = config.getStartPort(); port <= config.getEndPort(); port++) {
            if (cancelled.get()) {
                break;
            }
            ScanTask task = new ScanTask(targetAddress, port, config.getTimeoutMillis(),
                    config.isGrabBanners(), unreachableWarned);
            futures.add(executor.submit(task));
        }

        collectResults(futures, totalPorts);

        executorManager.shutdown();
        long duration = System.currentTimeMillis() - scanStartMillis;

        boolean wasCancelled = cancelled.get();
        AppLogger.info("Scan %s. %d/%d ports processed in %dms."
                .formatted(wasCancelled ? "cancelled" : "completed", results.size(), totalPorts, duration));

        listener.onScanFinished(wasCancelled);

        List<ScanResult> snapshot;
        synchronized (results) {
            snapshot = new ArrayList<>(results);
        }
        return new ScanStatistics(snapshot, duration);
    }

    private void collectResults(List<Future<ScanResult>> futures, int totalPorts) {
        for (Future<ScanResult> future : futures) {
            if (cancelled.get()) {
                future.cancel(true);
                continue;
            }
            try {
                ScanResult result = future.get();
                results.add(result);
                int completed = completedCount.incrementAndGet();
                listener.onPortScanned(result, completed, totalPorts);
            } catch (CancellationException ex) {
                // Expected when a scan is stopped mid-flight; nothing to report.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                cancelled.set(true);
            } catch (ExecutionException ex) {
                AppLogger.error("A scan task failed unexpectedly.", ex);
            }
        }
    }

    /**
     * Requests cancellation of the running scan. Safe to call from any thread
     * (e.g. the JavaFX Application Thread reacting to a "Stop Scan" button).
     */
    public void cancel() {
        cancelled.set(true);
        if (executorManager != null) {
            executorManager.shutdownNow();
        }
        AppLogger.warn("Scan cancellation requested by user.");
    }

    public boolean isCancelled() {
        return cancelled.get();
    }
}
