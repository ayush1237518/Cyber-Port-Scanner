package com.portscanner.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Attempts to retrieve a short service banner from an already-open TCP socket.
 * <p>
 * Some services (SSH, FTP, SMTP...) announce themselves immediately upon
 * connection. Others (HTTP) require a request to be sent first before they
 * reply. This class handles both cases and never throws: any failure simply
 * results in an empty banner, since banner grabbing is a best-effort
 * enhancement and must never crash a scan.
 */
public final class BannerGrabber {

    private static final int BANNER_BUFFER_SIZE = 1024;
    private static final int BANNER_READ_TIMEOUT_MS = 800;

    /** Ports that require an active probe (e.g. a minimal HTTP request) to elicit a response. */
    private static final int HTTP_PORT = 80;
    private static final int HTTP_ALT_PORT = 8080;
    private static final int HTTPS_PORT = 443;

    private BannerGrabber() {
        // Utility class - no instances.
    }

    /**
     * Attempts to read a banner from the given socket. The socket is assumed
     * to already be connected; this method does not close it.
     *
     * @param socket an open, connected socket
     * @param port   the remote port, used to decide whether an active probe is needed
     * @return the banner text (trimmed, single line), or an empty string if none was retrieved
     */
    public static String grab(Socket socket, int port) {
        try {
            socket.setSoTimeout(BANNER_READ_TIMEOUT_MS);

            if (port == HTTP_PORT || port == HTTP_ALT_PORT) {
                sendMinimalHttpProbe(socket);
            } else if (port == HTTPS_PORT) {
                // Reading a raw TLS handshake as text is not meaningful; skip actively probing.
                return "";
            }

            return readLine(socket);
        } catch (IOException ex) {
            // Filtered, closed early, or simply silent — this is expected and not an error.
            return "";
        }
    }

    private static void sendMinimalHttpProbe(Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        String request = "HEAD / HTTP/1.0\r\n\r\n";
        out.write(request.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static String readLine(Socket socket) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))) {

            char[] buffer = new char[BANNER_BUFFER_SIZE];
            int charsRead = reader.read(buffer);
            if (charsRead <= 0) {
                return "";
            }
            String raw = new String(buffer, 0, charsRead);
            // Collapse to the first non-blank line and strip control characters for safe display.
            String firstLine = raw.lines().filter(l -> !l.isBlank()).findFirst().orElse("");
            return sanitize(firstLine);
        }
    }

    /**
     * Strips non-printable characters so banners can be safely rendered in the
     * GUI and written into CSV/JSON/TXT reports without corrupting them.
     */
    private static String sanitize(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 0x20 && c < 0x7F) {
                sb.append(c);
            }
        }
        String result = sb.toString().trim();
        return result.length() > 120 ? result.substring(0, 120) + "..." : result;
    }
}
