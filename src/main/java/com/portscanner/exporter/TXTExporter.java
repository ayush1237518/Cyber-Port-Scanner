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
 * Exports a human-friendly, plain-text scan report — the kind that reads
 * well when pasted directly into a ticket, email, or lab writeup.
 */
public final class TXTExporter implements ReportExporter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARATOR = "=".repeat(70);

    @Override
    public void export(Path destination, ScanConfig config, List<ScanResult> results,
                        ScanStatistics statistics) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(destination)) {
            writer.write(SEPARATOR);
            writer.newLine();
            writer.write("           NETWORK PORT SCANNER - SCAN REPORT");
            writer.newLine();
            writer.write(SEPARATOR);
            writer.newLine();
            writer.write("Generated : " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
            writer.newLine();
            writer.write("Target    : " + config.getTarget());
            writer.newLine();
            writer.write("Port Range: %d - %d".formatted(config.getStartPort(), config.getEndPort()));
            writer.newLine();
            writer.write("Timeout   : %d ms".formatted(config.getTimeoutMillis()));
            writer.newLine();
            writer.write("Threads   : %d".formatted(config.getThreadCount()));
            writer.newLine();
            writer.write("Duration  : " + TimerUtil.formatDuration(statistics.getScanDurationMillis()));
            writer.newLine();
            writer.newLine();

            writer.write("SUMMARY");
            writer.newLine();
            writer.write("-".repeat(70));
            writer.newLine();
            writer.write("Total Ports Scanned : " + statistics.getTotalPorts());
            writer.newLine();
            writer.write("Open Ports           : " + statistics.getOpenCount());
            writer.newLine();
            writer.write("Closed Ports         : " + statistics.getClosedCount());
            writer.newLine();
            writer.write("Filtered Ports       : " + statistics.getFilteredCount());
            writer.newLine();
            writer.write("Errors               : " + statistics.getErrorCount());
            writer.newLine();
            writer.newLine();

            writer.write("DETAILED RESULTS");
            writer.newLine();
            writer.write("-".repeat(70));
            writer.newLine();
            writer.write("%-8s %-10s %-16s %-12s %s".formatted("PORT", "STATUS", "SERVICE", "TIME(ms)", "BANNER"));
            writer.newLine();

            for (ScanResult result : results) {
                writer.write("%-8d %-10s %-16s %-12d %s".formatted(
                        result.getPort(),
                        result.getStatus().getDisplayName(),
                        result.getServiceName(),
                        result.getResponseTimeMillis(),
                        result.hasBanner() ? result.getBanner() : "-"
                ));
                writer.newLine();
            }

            writer.newLine();
            writer.write(SEPARATOR);
            writer.newLine();
            writer.write("End of report.");
            writer.newLine();
        }
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }
}
