package com.portscanner.exporter;

import com.portscanner.model.ScanConfig;
import com.portscanner.model.ScanResult;
import com.portscanner.model.ScanStatistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports scan reports as JSON.
 * <p>
 * Implemented by hand rather than pulling in a JSON library, since the
 * document shape is small and fixed; this keeps the project dependency-free
 * for a portfolio build. All string values are escaped per the JSON spec.
 */
public final class JSONExporter implements ReportExporter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public void export(Path destination, ScanConfig config, List<ScanResult> results,
                        ScanStatistics statistics) throws IOException {

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"generatedAt\": \"").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("\",\n");
        json.append("  \"target\": \"").append(escape(config.getTarget())).append("\",\n");
        json.append("  \"scanConfiguration\": {\n");
        json.append("    \"startPort\": ").append(config.getStartPort()).append(",\n");
        json.append("    \"endPort\": ").append(config.getEndPort()).append(",\n");
        json.append("    \"timeoutMs\": ").append(config.getTimeoutMillis()).append(",\n");
        json.append("    \"threadCount\": ").append(config.getThreadCount()).append(",\n");
        json.append("    \"bannerGrabbingEnabled\": ").append(config.isGrabBanners()).append("\n");
        json.append("  },\n");
        json.append("  \"statistics\": {\n");
        json.append("    \"totalPorts\": ").append(statistics.getTotalPorts()).append(",\n");
        json.append("    \"openPorts\": ").append(statistics.getOpenCount()).append(",\n");
        json.append("    \"closedPorts\": ").append(statistics.getClosedCount()).append(",\n");
        json.append("    \"filteredPorts\": ").append(statistics.getFilteredCount()).append(",\n");
        json.append("    \"errorPorts\": ").append(statistics.getErrorCount()).append(",\n");
        json.append("    \"scanDurationMs\": ").append(statistics.getScanDurationMillis()).append("\n");
        json.append("  },\n");
        json.append("  \"results\": [\n");

        for (int i = 0; i < results.size(); i++) {
            json.append(toJsonObject(results.get(i)));
            if (i < results.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        try (BufferedWriter writer = Files.newBufferedWriter(destination)) {
            writer.write(json.toString());
        }
    }

    private String toJsonObject(ScanResult result) {
        return """
                    {
                      "port": %d,
                      "status": "%s",
                      "service": "%s",
                      "responseTimeMs": %d,
                      "banner": "%s"
                    }""".formatted(
                result.getPort(),
                result.getStatus().getDisplayName(),
                escape(result.getServiceName()),
                result.getResponseTimeMillis(),
                escape(result.getBanner())
        );
    }

    /**
     * Escapes a string for safe inclusion in a JSON document per RFC 8259.
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String getFileExtension() {
        return "json";
    }
}
