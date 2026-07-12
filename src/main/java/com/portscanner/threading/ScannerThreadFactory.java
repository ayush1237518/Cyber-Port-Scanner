package com.portscanner.threading;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} that produces clearly-named daemon threads for the
 * scanner's worker pool. Naming threads makes profiling and debugging live
 * scans (e.g. via jstack) far easier than generic "pool-1-thread-N" names.
 */
public final class ScannerThreadFactory implements ThreadFactory {

    private final AtomicInteger threadCounter = new AtomicInteger(1);
    private final String poolName;

    public ScannerThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "%s-worker-%d".formatted(poolName, threadCounter.getAndIncrement()));
        // Daemon threads ensure the JVM can still exit if a scan is abandoned
        // without an explicit shutdown (e.g. abrupt window close).
        thread.setDaemon(true);
        return thread;
    }
}
