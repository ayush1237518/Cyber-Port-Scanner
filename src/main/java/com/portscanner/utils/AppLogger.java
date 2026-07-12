package com.portscanner.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * A minimal, dependency-free, thread-safe logger.
 * <p>
 * Rather than pulling in a full logging framework for a portfolio project,
 * this class provides just what's needed: timestamped console output, an
 * optional rolling log file under {@code logs/}, and the ability to register
 * a UI callback so log lines can be streamed live into the GUI's logging panel.
 */
public final class AppLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Path LOG_DIRECTORY = Path.of("logs");
    private static final Object FILE_LOCK = new Object();

    private static volatile Consumer<String> uiSink;
    private static PrintWriter fileWriter;

    static {
        initializeLogFile();
    }

    private AppLogger() {
        // Utility class - no instances.
    }

    private static void initializeLogFile() {
        try {
            Files.createDirectories(LOG_DIRECTORY);
            String fileName = "scan-%s.log".formatted(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            fileWriter = new PrintWriter(Files.newBufferedWriter(LOG_DIRECTORY.resolve(fileName)), true);
        } catch (IOException ex) {
            // If the log file cannot be created (e.g. read-only filesystem),
            // fall back to console-only logging rather than crashing the app.
            fileWriter = null;
        }
    }

    /**
     * Registers a callback that receives every future log line. Used by the
     * UI layer to pipe messages into the real-time logging panel.
     */
    public static void setUiSink(Consumer<String> sink) {
        uiSink = sink;
    }

    public static void info(String message) {
        write("INFO", message);
    }

    public static void warn(String message) {
        write("WARN", message);
    }

    public static void error(String message) {
        write("ERROR", message);
    }

    public static void error(String message, Throwable throwable) {
        write("ERROR", message + " :: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
    }

    private static void write(String level, String message) {
        String line = "[%s] [%s] %s".formatted(LocalDateTime.now().format(TIMESTAMP_FORMAT), level, message);

        System.out.println(line);

        synchronized (FILE_LOCK) {
            if (fileWriter != null) {
                fileWriter.println(line);
            }
        }

        Consumer<String> sink = uiSink;
        if (sink != null) {
            sink.accept(line);
        }
    }
}
