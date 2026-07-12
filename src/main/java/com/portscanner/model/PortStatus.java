package com.portscanner.model;

/**
 * Represents the possible outcomes of a single port scan attempt.
 *
 * <ul>
 *     <li>{@link #OPEN}     - A TCP connection was successfully established.</li>
 *     <li>{@link #CLOSED}   - The target actively refused the connection (RST received).</li>
 *     <li>{@link #FILTERED} - No response was received within the timeout window,
 *                             typically indicating a firewall is silently dropping packets.</li>
 *     <li>{@link #ERROR}    - An unexpected I/O error occurred that is not a standard
 *                             closed/filtered condition (e.g. DNS failure).</li>
 * </ul>
 */
public enum PortStatus {

    OPEN("Open"),
    CLOSED("Closed"),
    FILTERED("Filtered"),
    ERROR("Error");

    private final String displayName;

    PortStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return a human-readable label suitable for UI display and reports.
     */
    public String getDisplayName() {
        return displayName;
    }
}
