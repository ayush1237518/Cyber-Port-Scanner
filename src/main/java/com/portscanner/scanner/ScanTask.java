package com.portscanner.scanner;

import com.portscanner.model.PortStatus;
import com.portscanner.model.ScanResult;
import com.portscanner.services.BannerGrabber;
import com.portscanner.services.ServiceDetector;

import com.portscanner.utils.AppLogger;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A single unit of scanning work: attempts a TCP connect to one port on one
 * host, classifies the outcome, and optionally grabs a banner if the port is
 * open. Designed to be submitted to an {@link java.util.concurrent.ExecutorService}.
 * <p>
 * Each task is fully self-contained and touches no shared mutable state,
 * which makes the overall scan trivially thread-safe.
 */
public final class ScanTask implements Callable<ScanResult> {

    private final InetSocketAddress targetAddress;
    private final int port;
    private final int timeoutMillis;
    private final boolean grabBanners;
    private final AtomicBoolean unreachableWarned;

    public ScanTask(InetSocketAddress targetAddress, int port, int timeoutMillis, boolean grabBanners) {
        this(targetAddress, port, timeoutMillis, grabBanners, new AtomicBoolean(false));
    }

    /**
     * @param unreachableWarned shared across every {@code ScanTask} in the same scan, so a
     *                          host-unreachable condition (which affects every port identically)
     *                          is logged once per scan rather than once per port.
     */
    public ScanTask(InetSocketAddress targetAddress, int port, int timeoutMillis, boolean grabBanners,
                     AtomicBoolean unreachableWarned) {
        this.targetAddress = targetAddress;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
        this.grabBanners = grabBanners;
        this.unreachableWarned = unreachableWarned;
    }

    @Override
    public ScanResult call() {
        long startNanos = System.nanoTime();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetAddress.getAddress(), port), timeoutMillis);

            long responseTime = elapsedMillis(startNanos);
            String service = ServiceDetector.identify(port);
            String banner = grabBanners ? BannerGrabber.grab(socket, port) : "";

            return new ScanResult(port, PortStatus.OPEN, service, responseTime, banner);

        } catch (SocketTimeoutException timeout) {
            // No response within the timeout window: most likely firewall-filtered.
            return new ScanResult(port, PortStatus.FILTERED, ServiceDetector.identify(port),
                    elapsedMillis(startNanos), "");

        } catch (ConnectException refused) {
            // Target actively refused the connection (TCP RST): the port is closed.
            return new ScanResult(port, PortStatus.CLOSED, ServiceDetector.identify(port),
                    elapsedMillis(startNanos), "");

        } catch (NoRouteToHostException unreachable) {
            // The host itself is unreachable (bad route, dead host, blocked upstream).
            // This is NOT the same thing as "filtered": every port on this host will fail
            // identically, which is exactly what makes an unreachable host indistinguishable
            // from a clean 0-open/0-closed/0-filtered scan unless we say so explicitly.
            // Logged once per scan (not once per port) to keep the log panel readable.
            if (unreachableWarned.compareAndSet(false, true)) {
                AppLogger.warn(("Host unreachable (%s). Every port on this target will show as " +
                        "an error for the same reason — check routing/VPN/firewall to the target, " +
                        "not the scanner logic.").formatted(unreachable.getMessage()));
            }
            return new ScanResult(port, PortStatus.ERROR, ServiceDetector.identify(port),
                    elapsedMillis(startNanos), "");

        } catch (Exception unexpected) {
            // Any other I/O failure (e.g. network unreachable, permission denied) is
            // reported distinctly so it isn't silently misclassified as "closed", and
            // logged with its real cause (once per scan) so it's diagnosable instead of invisible.
            if (unreachableWarned.compareAndSet(false, true)) {
                AppLogger.warn("Scan error on port %d - %s: %s"
                        .formatted(port, unexpected.getClass().getSimpleName(), unexpected.getMessage()));
            }
            return new ScanResult(port, PortStatus.ERROR, ServiceDetector.identify(port),
                    elapsedMillis(startNanos), "");
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
