package com.portscanner.model;

import java.util.List;

/**
 * Aggregate statistics computed once a scan completes (or is cancelled).
 * Built from a finished list of {@link ScanResult} objects, so it is
 * calculated once and treated as an immutable snapshot.
 */
public final class ScanStatistics {

    private final int totalPorts;
    private final long openCount;
    private final long closedCount;
    private final long filteredCount;
    private final long errorCount;
    private final long scanDurationMillis;

    public ScanStatistics(List<ScanResult> results, long scanDurationMillis) {
        this.totalPorts = results.size();
        this.openCount = results.stream().filter(r -> r.getStatus() == PortStatus.OPEN).count();
        this.closedCount = results.stream().filter(r -> r.getStatus() == PortStatus.CLOSED).count();
        this.filteredCount = results.stream().filter(r -> r.getStatus() == PortStatus.FILTERED).count();
        this.errorCount = results.stream().filter(r -> r.getStatus() == PortStatus.ERROR).count();
        this.scanDurationMillis = scanDurationMillis;
    }

    public int getTotalPorts() {
        return totalPorts;
    }

    public long getOpenCount() {
        return openCount;
    }

    public long getClosedCount() {
        return closedCount;
    }

    public long getFilteredCount() {
        return filteredCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getScanDurationMillis() {
        return scanDurationMillis;
    }

    public double getScanDurationSeconds() {
        return scanDurationMillis / 1000.0;
    }
}
