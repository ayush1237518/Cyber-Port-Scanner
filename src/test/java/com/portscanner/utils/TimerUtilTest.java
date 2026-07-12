package com.portscanner.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link TimerUtil} duration formatting and progress estimation.
 */
class TimerUtilTest {

    @Test
    void formatsDurationCorrectly() {
        assertEquals("00:00:05", TimerUtil.formatDuration(5_000));
        assertEquals("00:01:00", TimerUtil.formatDuration(60_000));
        assertEquals("01:00:00", TimerUtil.formatDuration(3_600_000));
    }

    @Test
    void calculatesPercentComplete() {
        assertEquals(50.0, TimerUtil.percentComplete(50, 100));
        assertEquals(0.0, TimerUtil.percentComplete(0, 100));
        assertEquals(100.0, TimerUtil.percentComplete(100, 100));
        assertEquals(0.0, TimerUtil.percentComplete(10, 0));
    }

    @Test
    void estimatesRemainingTimeLinearly() {
        // 10 items took 1000ms -> 100ms/item; 90 remain -> 9000ms estimated
        long remaining = TimerUtil.estimateRemainingMillis(1000, 10, 100);
        assertEquals(9000, remaining);
    }

    @Test
    void returnsZeroRemainingWhenComplete() {
        assertEquals(0, TimerUtil.estimateRemainingMillis(1000, 100, 100));
        assertEquals(0, TimerUtil.estimateRemainingMillis(1000, 0, 100));
    }
}
