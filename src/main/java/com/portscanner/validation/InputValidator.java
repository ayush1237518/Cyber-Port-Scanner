package com.portscanner.validation;

import com.portscanner.model.ScanConfig;

import java.net.IDN;
import java.util.regex.Pattern;

/**
 * Validates raw string input from the UI before it is turned into a
 * {@link ScanConfig}. Centralising validation here keeps the UI layer free
 * of business rules and makes the rules independently unit-testable.
 */
public final class InputValidator {

    /** Reasonable upper bound so a user cannot accidentally spin up an unusable thread pool. */
    public static final int MAX_THREADS = 500;
    public static final int MIN_THREADS = 1;

    public static final int MIN_TIMEOUT_MS = 50;
    public static final int MAX_TIMEOUT_MS = 20_000;

    public static final int MIN_PORT = 1;
    public static final int MAX_PORT = 65_535;

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$"
    );

    /**
     * Hostnames: letters, digits, hyphens, dots. Must not start/end with a hyphen or dot,
     * and each label must be 1-63 characters, matching RFC 1123 practical constraints.
     */
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
            "^(?=.{1,253}$)([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)*[a-zA-Z]{2,63}$"
    );

    private InputValidator() {
        // Utility class - no instances.
    }

    /**
     * Validates a target host/IP string.
     *
     * @param target raw user input
     * @return the trimmed, validated target
     * @throws ValidationException if the target is blank or structurally invalid
     */
    public static String validateTarget(String target) throws ValidationException {
        if (target == null || target.isBlank()) {
            throw new ValidationException("Target IP address or hostname must not be empty.");
        }
        String trimmed = target.trim();

        if (IPV4_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        // Convert internationalized domain names to ASCII before validating shape.
        String asciiForm;
        try {
            asciiForm = IDN.toASCII(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Target '%s' is not a valid IPv4 address or hostname.".formatted(trimmed));
        }

        if (!HOSTNAME_PATTERN.matcher(asciiForm).matches()) {
            throw new ValidationException("Target '%s' is not a valid IPv4 address or hostname.".formatted(trimmed));
        }
        return trimmed;
    }

    /**
     * Validates that a port number falls within the legal TCP port range.
     *
     * @param port     the port value to validate
     * @param fieldName label used in the error message (e.g. "Start port")
     * @throws ValidationException if out of range
     */
    public static int validatePort(int port, String fieldName) throws ValidationException {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new ValidationException(
                    "%s must be between %d and %d.".formatted(fieldName, MIN_PORT, MAX_PORT));
        }
        return port;
    }

    /**
     * Validates that the start port does not exceed the end port.
     */
    public static void validatePortRange(int startPort, int endPort) throws ValidationException {
        if (startPort > endPort) {
            throw new ValidationException("Start port (%d) cannot be greater than end port (%d)."
                    .formatted(startPort, endPort));
        }
    }

    public static int validateTimeout(int timeoutMillis) throws ValidationException {
        if (timeoutMillis < MIN_TIMEOUT_MS || timeoutMillis > MAX_TIMEOUT_MS) {
            throw new ValidationException("Timeout must be between %d and %d milliseconds."
                    .formatted(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS));
        }
        return timeoutMillis;
    }

    public static int validateThreadCount(int threadCount) throws ValidationException {
        if (threadCount < MIN_THREADS || threadCount > MAX_THREADS) {
            throw new ValidationException("Thread count must be between %d and %d."
                    .formatted(MIN_THREADS, MAX_THREADS));
        }
        return threadCount;
    }

    /**
     * Parses and validates a string as an integer, producing a friendly error
     * message on failure instead of letting a raw {@link NumberFormatException} escape.
     */
    public static int parseIntField(String rawValue, String fieldName) throws ValidationException {
        if (rawValue == null || rawValue.isBlank()) {
            throw new ValidationException("%s must not be empty.".formatted(fieldName));
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("%s must be a whole number.".formatted(fieldName));
        }
    }

    /**
     * Runs the full validation pipeline and builds a {@link ScanConfig} from raw UI strings.
     * This is the single entry point the UI layer should call.
     */
    public static ScanConfig buildValidatedConfig(String target, String startPortRaw, String endPortRaw,
                                                   String timeoutRaw, String threadCountRaw,
                                                   boolean grabBanners) throws ValidationException {
        String validTarget = validateTarget(target);

        int startPort = validatePort(parseIntField(startPortRaw, "Start port"), "Start port");
        int endPort = validatePort(parseIntField(endPortRaw, "End port"), "End port");
        validatePortRange(startPort, endPort);

        int timeout = validateTimeout(parseIntField(timeoutRaw, "Timeout"));
        int threads = validateThreadCount(parseIntField(threadCountRaw, "Thread count"));

        return new ScanConfig(validTarget, startPort, endPort, timeout, threads, grabBanners);
    }
}
