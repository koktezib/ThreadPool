package org.example;

import org.example.CustomExecutor;
import org.example.CustomThreadFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPool implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueSize;
    private final int minSpareThreads;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;

    private final List<Worker> workers = Collections.synchronizedList(new ArrayList<>());
    private final List<BlockingQueue<Runnable>> queues;
    private final CustomThreadFactory threadFactory;
    private final AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean shutdown = false;

    public CustomThreadPool(int corePoolSize, int maxPoolSize, int queueSize, long keepAliveTime, TimeUnit timeUnit, int minSpareThreads, String poolName) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueSize = queueSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.minSpareThreads = minSpareThreads;
        this.threadFactory = new CustomThreadFactory(poolName);

        // По очереди на каждого воркера
        queues = new ArrayList<>();
        for (int i = 0; i < maxPoolSize; i++) {
            queues.add(new LinkedBlockingQueue<>(queueSize));
        }
        // Инициализация corePoolSize воркеров
        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }
    }

    private void addWorker() {
        if (workers.size() >= maxPoolSize) return;
        Worker w = new Worker(queues.get(workers.size()));
        Thread t = threadFactory.newThread(w);
        w.thread = t;
        workers.add(w);
        t.start();
    }

    private BlockingQueue<Runnable> nextQueue() {
        int idx = roundRobin.getAndIncrement() % workers.size();
        return queues.get(idx);
    }

    @Override
    public void execute(Runnable command) {
        if (shutdown) {
            System.out.println("[Rejected] Task <" + command + "> was rejected: pool is shutting down.");
            throw new RejectedExecutionException("Pool is shutting down");
        }
        boolean accepted = false;
        int attempt = 0;
        while (!accepted && attempt < workers.size()) {
            BlockingQueue<Runnable> queue = queues.get(attempt % workers.size());
            if (queue.offer(command)) {
                System.out.println("[Pool] Task accepted into queue #" + attempt + ": " + command);
                accepted = true;
                break;
            }
            attempt++;
        }
        if (!accepted) {
            // Отказ: задача не принята ни одной очередью
            System.out.println("[Rejected] Task <" + command + "> was rejected due to overload!");
            throw new RejectedExecutionException("All queues are overloaded");
        }
        // Динамическое добавление воркера при нехватке свободных (minSpareThreads)
        ensureSpareThreads();
    }

    private void ensureSpareThreads() {
        // Считаем свободные потоки (у которых очередь пуста)
        long free = workers.stream().filter(w -> w.queue.isEmpty()).count();
        while (free < minSpareThreads && workers.size() < maxPoolSize) {
            addWorker();
            free++;
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        execute(task);
        return task;
    }

    @Override
    public void shutdown() {
        shutdown = true;
        System.out.println("[Pool] Shutdown initiated.");
    }

    @Override
    public void shutdownNow() {
        shutdown = true;
        System.out.println("[Pool] Immediate shutdown initiated.");
        synchronized (workers) {
            for (Worker w : workers) {
                w.thread.interrupt();
            }
        }
    }

    // Рабочий поток
    private class Worker implements Runnable {
        private final BlockingQueue<Runnable> queue;
        private Thread thread;

        public Worker(BlockingQueue<Runnable> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            try {
                while (!shutdown || !queue.isEmpty()) {
                    Runnable task = null;
                    try {
                        task = queue.poll(keepAliveTime, timeUnit);
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (task != null) {
                        System.out.println("[Worker] " + name + " executes " + task);
                        try {
                            task.run();
                        } catch (Throwable t) {
                            System.err.println("[Worker] Exception in task: " + t);
                        }
                    } else {
                        // Idle timeout
                        if (workers.size() > corePoolSize) {
                            System.out.println("[Worker] " + name + " idle timeout, stopping.");
                            break;
                        }
                    }
                }
            } finally {
                workers.remove(this);
                System.out.println("[Worker] " + name + " terminated.");
            }
        }
    }
}
