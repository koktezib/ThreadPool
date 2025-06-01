package org.example;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {
    private final String poolName;
    private final AtomicInteger count = new AtomicInteger(1);

    public CustomThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = poolName + "-worker-" + count.getAndIncrement();
        System.out.println("[ThreadFactory] Creating new thread: " + threadName);
        Thread t = new Thread(r, threadName);
        t.setUncaughtExceptionHandler((th, ex) -> {
            System.err.println("[ThreadFactory] Uncaught exception in " + th.getName() + ": " + ex);
        });
        return t;
    }
}
