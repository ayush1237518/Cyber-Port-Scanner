package com.portscanner.exporter;

import com.portscanner.model.ScanConfig;
import com.portscanner.model.ScanResult;
import com.portscanner.model.ScanStatistics;
import com.portscanner.utils.TimerUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports scan reports in CSV format, suitable for opening in Excel/Sheets
 * or for feeding into other tooling.
 */
public final class CSVExporter implements ReportExporter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void export(Path destination, ScanConfig config, List<ScanResult> results,
                        ScanStatistics statistics) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(destination)) {
            writeMetadataHeader(writer, config, statistics);
            writer.write("Port,Status,Service,ResponseTimeMs,Banner");
            writer.newLine();

            for (ScanResult result : results) {
                writer.write(toCsvRow(result));
                writer.newLine();
            }
        }
    }

    private void writeMetadataHeader(BufferedWriter writer, ScanConfig config, ScanStatistics stats)
            throws IOException {
        writer.write("# Network Port Scanner Report");
        writer.newLine();
        writer.write("# Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        writer.newLine();
        writer.write("# Target: " + config.getTarget());
        writer.newLine();
        writer.write("# Port Range: " + config.getStartPort() + "-" + config.getEndPort());
        writer.newLine();
        writer.write("# Timeout(ms): " + config.getTimeoutMillis());
        writer.newLine();
        writer.write("# Threads: " + config.getThreadCount());
        writer.newLine();
        writer.write("# Duration: " + TimerUtil.formatDuration(stats.getScanDurationMillis()));
        writer.newLine();
        writer.write("# Total: %d, Open: %d, Closed: %d, Filtered: %d, Errors: %d".formatted(
                stats.getTotalPorts(), stats.getOpenCount(), stats.getClosedCount(),
                stats.getFilteredCount(), stats.getErrorCount()));
        writer.newLine();
        writer.write("#");
        writer.newLine();
    }

    private String toCsvRow(ScanResult result) {
        return "%d,%s,%s,%d,%s".formatted(
                result.getPort(),
                result.getStatus().getDisplayName(),
                escapeCsv(result.getServiceName()),
                result.getResponseTimeMillis(),
                escapeCsv(result.getBanner())
        );
    }

    /**
     * Escapes a CSV field: wraps in quotes and doubles any embedded quotes
     * whenever the field contains a comma, quote, or newline.
     */
    private String escapeCsv(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }
}
