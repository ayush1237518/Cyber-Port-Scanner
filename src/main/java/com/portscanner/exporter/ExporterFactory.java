package com.portscanner.exporter;

/**
 * Factory that produces the appropriate {@link ReportExporter} for a
 * requested {@link ExportFormat}. Keeps the UI layer decoupled from the
 * concrete exporter classes (Dependency Inversion).
 */
public final class ExporterFactory {

    public enum ExportFormat {
        CSV, JSON, TXT
    }

    private ExporterFactory() {
        // Utility class - no instances.
    }

    public static ReportExporter create(ExportFormat format) {
        return switch (format) {
            case CSV -> new CSVExporter();
            case JSON -> new JSONExporter();
            case TXT -> new TXTExporter();
        };
    }
}
