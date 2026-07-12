package com.portscanner.model;

/**
 * Immutable configuration describing a single scan job.
 * <p>
 * Instances should only be constructed after passing through
 * {@code com.portscanner.validation.InputValidator}, so this class assumes
 * its inputs are already sane and focuses purely on data transport.
 */
public final class ScanConfig {

    private final String target;
    private final int startPort;
    private final int endPort;
    private final int timeoutMillis;
    private final int threadCount;
    private final boolean grabBanners;

    public ScanConfig(String target, int startPort, int endPort,
                       int timeoutMillis, int threadCount, boolean grabBanners) {
        this.target = target;
        this.startPort = startPort;
        this.endPort = endPort;
        this.timeoutMillis = timeoutMillis;
        this.threadCount = threadCount;
        this.grabBanners = grabBanners;
    }

    public String getTarget() {
        return target;
    }

    public int getStartPort() {
        return startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public boolean isGrabBanners() {
        return grabBanners;
    }

    public int getTotalPorts() {
        return (endPort - startPort) + 1;
    }

    @Override
    public String toString() {
        return "ScanConfig{target='%s', ports=%d-%d, timeout=%dms, threads=%d, banners=%b}"
                .formatted(target, startPort, endPort, timeoutMillis, threadCount, grabBanners);
    }
}
