package com.portscanner.scanner;

import com.portscanner.model.ScanResult;

/**
 * Callback contract used by {@link PortScanner} to report live progress
 * and per-port results back to the caller (typically a JavaFX controller).
 * <p>
 * Implementations must be careful about thread affinity: these callbacks
 * fire from worker threads, NOT the JavaFX Application Thread, so UI
 * implementations must marshal updates via {@code Platform.runLater}.
 */
public interface ScanProgressListener {

    /**
     * Invoked once for every port as soon as its result is known.
     */
    void onPortScanned(ScanResult result, int completedCount, int totalCount);

    /**
     * Invoked when the entire scan job has finished, whether naturally
     * completed or stopped early via cancellation.
     */
    void onScanFinished(boolean wasCancelled);

    /**
     * Invoked if the scan could not proceed at all (e.g. DNS resolution failure).
     */
    void onScanFailed(String reason);
}
