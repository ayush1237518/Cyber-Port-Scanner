package com.portscanner.model;

import java.util.Objects;

/**
 * Immutable value object representing the result of scanning a single TCP port.
 * <p>
 * Instances are created once per scanned port and are safe to share across
 * threads because all fields are final and the class exposes no mutators.
 */
public final class ScanResult implements Comparable<ScanResult> {

    private final int port;
    private final PortStatus status;
    private final String serviceName;
    private final long responseTimeMillis;
    private final String banner;

    public ScanResult(int port, PortStatus status, String serviceName,
                       long responseTimeMillis, String banner) {
        this.port = port;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.serviceName = (serviceName == null || serviceName.isBlank()) ? "Unknown" : serviceName;
        this.responseTimeMillis = responseTimeMillis;
        this.banner = (banner == null || banner.isBlank()) ? "" : banner;
    }

    public int getPort() {
        return port;
    }

    public PortStatus getStatus() {
        return status;
    }

    public String getServiceName() {
        return serviceName;
    }

    public long getResponseTimeMillis() {
        return responseTimeMillis;
    }

    public String getBanner() {
        return banner;
    }

    public boolean hasBanner() {
        return !banner.isEmpty();
    }

    /**
     * Natural ordering is by port number ascending, matching the default
     * "sort by port" behaviour requested in the UI.
     */
    @Override
    public int compareTo(ScanResult other) {
        return Integer.compare(this.port, other.port);
    }

    @Override
    public String toString() {
        return "ScanResult{port=%d, status=%s, service='%s', responseTime=%dms, banner='%s'}"
                .formatted(port, status, serviceName, responseTimeMillis, banner);
    }
}
