package com.portscanner.exporter;

import com.portscanner.model.ScanConfig;
import com.portscanner.model.ScanResult;
import com.portscanner.model.ScanStatistics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Common contract implemented by every report exporter (CSV, JSON, TXT).
 * <p>
 * Following the Strategy pattern here means the UI layer can treat all export
 * formats uniformly and new formats can be added without touching existing code
 * (Open/Closed Principle).
 */
public interface ReportExporter {

    /**
     * Writes a full scan report to the given file path.
     *
     * @param destination   the file to write to (parent directories must already exist)
     * @param config        the scan configuration that produced these results
     * @param results       the individual per-port results
     * @param statistics    aggregate scan statistics
     * @throws IOException if the file cannot be written
     */
    void export(Path destination, ScanConfig config, List<ScanResult> results, ScanStatistics statistics)
            throws IOException;

    /**
     * @return the file extension (without a leading dot) this exporter produces, e.g. "csv"
     */
    String getFileExtension();
}
