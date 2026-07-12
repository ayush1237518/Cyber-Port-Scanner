package com.portscanner.services;

import java.util.Map;

/**
 * Resolves a TCP port number to its commonly associated service name,
 * based on the IANA well-known ports registry (a practical, curated subset
 * covering the services most relevant to network reconnaissance).
 * <p>
 * This is a best-effort lookup only: a service listening on a non-standard
 * port will not be identified by number alone. Combine with
 * {@link BannerGrabber} for higher-confidence identification.
 */
public final class ServiceDetector {

    private static final Map<Integer, String> WELL_KNOWN_PORTS = Map.ofEntries(
            Map.entry(20, "FTP-DATA"),
            Map.entry(21, "FTP"),
            Map.entry(22, "SSH"),
            Map.entry(23, "Telnet"),
            Map.entry(25, "SMTP"),
            Map.entry(53, "DNS"),
            Map.entry(80, "HTTP"),
            Map.entry(110, "POP3"),
            Map.entry(111, "RPCbind"),
            Map.entry(135, "MSRPC"),
            Map.entry(139, "NetBIOS-SSN"),
            Map.entry(143, "IMAP"),
            Map.entry(161, "SNMP"),
            Map.entry(389, "LDAP"),
            Map.entry(443, "HTTPS"),
            Map.entry(445, "SMB"),
            Map.entry(465, "SMTPS"),
            Map.entry(587, "SMTP-Submission"),
            Map.entry(631, "IPP"),
            Map.entry(993, "IMAPS"),
            Map.entry(995, "POP3S"),
            Map.entry(1433, "MSSQL"),
            Map.entry(1521, "Oracle-DB"),
            Map.entry(3306, "MySQL"),
            Map.entry(3389, "RDP"),
            Map.entry(5432, "PostgreSQL"),
            Map.entry(5900, "VNC"),
            Map.entry(6379, "Redis"),
            Map.entry(8080, "HTTP-Alt"),
            Map.entry(8443, "HTTPS-Alt"),
            Map.entry(9200, "Elasticsearch"),
            Map.entry(27017, "MongoDB")
    );

    private ServiceDetector() {
        // Utility class - no instances.
    }

    /**
     * @param port the TCP port number
     * @return the commonly associated service name, or "Unknown" if unrecognized
     */
    public static String identify(int port) {
        return WELL_KNOWN_PORTS.getOrDefault(port, "Unknown");
    }
}
