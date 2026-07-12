package com.portscanner.scanner;

import com.portscanner.model.PortStatus;
import com.portscanner.model.ScanResult;
import com.portscanner.services.BannerGrabber;
import com.portscanner.services.ServiceDetector;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;

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

    public ScanTask(InetSocketAddress targetAddress, int port, int timeoutMillis, boolean grabBanners) {
        this.targetAddress = targetAddress;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
        this.grabBanners = grabBanners;
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

        } catch (Exception unexpected) {
            // Any other I/O failure (e.g. network unreachable) is reported distinctly
            // so it isn't silently misclassified as "closed".
            return new ScanResult(port, PortStatus.ERROR, ServiceDetector.identify(port),
                    elapsedMillis(startNanos), "");
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
