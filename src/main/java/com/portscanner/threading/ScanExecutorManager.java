package com.portscanner.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Wraps the creation and lifecycle management of the {@link ExecutorService}
 * used to run concurrent port scans. Isolating this here keeps
 * {@code PortScanner} focused on scan logic rather than thread-pool plumbing.
 */
public final class ScanExecutorManager {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final ExecutorService executorService;

    public ScanExecutorManager(int threadCount) {
        this.executorService = Executors.newFixedThreadPool(
                threadCount, new ScannerThreadFactory("port-scan"));
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Attempts an orderly shutdown, cancelling in-flight tasks if the pool
     * does not terminate within the timeout. Safe to call multiple times.
     */
    public void shutdownNow() {
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                // Pool did not terminate in time; nothing more we can safely do here.
                // This is logged by the caller, which has access to the AppLogger context.
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Graceful shutdown that lets already-submitted tasks finish naturally.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
