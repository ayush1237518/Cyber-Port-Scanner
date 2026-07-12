package com.portscanner.utils;

/**
 * Small helper for formatting durations and estimating remaining time
 * during a live scan. Kept separate from the UI so the estimation logic
 * is independently unit-testable.
 */
public final class TimerUtil {

    private TimerUtil() {
        // Utility class - no instances.
    }

    /**
     * Formats a millisecond duration as {@code HH:mm:ss} for display.
     */
    public static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
    }

    /**
     * Estimates remaining time based on progress so far, using a simple
     * linear extrapolation: (elapsed / completed) * remaining.
     *
     * @param elapsedMillis    time spent so far
     * @param completedItems   number of ports already scanned
     * @param totalItems       total number of ports to scan
     * @return estimated remaining milliseconds, or 0 if not enough data yet
     */
    public static long estimateRemainingMillis(long elapsedMillis, int completedItems, int totalItems) {
        if (completedItems <= 0 || totalItems <= 0 || completedItems >= totalItems) {
            return 0;
        }
        double averagePerItem = (double) elapsedMillis / completedItems;
        long remainingItems = totalItems - completedItems;
        return Math.round(averagePerItem * remainingItems);
    }

    /**
     * @return progress as a percentage (0-100), rounded to one decimal place.
     */
    public static double percentComplete(int completedItems, int totalItems) {
        if (totalItems <= 0) {
            return 0.0;
        }
        return Math.round(((double) completedItems / totalItems) * 1000.0) / 10.0;
    }
}
