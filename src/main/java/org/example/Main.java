package org.example;

import org.example.CustomThreadPool;

import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws Exception {
        CustomThreadPool pool = new CustomThreadPool(
                2,     // corePoolSize
                4,     // maxPoolSize
                5,     // queueSize
                5,     // keepAliveTime
                TimeUnit.SECONDS,
                1,     // minSpareThreads
                "MyPool"
        );

        int totalThreads = 23;
        // Задачи-имитаторы
        // Для проверки отказа нужно указать totalThreads > 23
        for (int i = 0; i < totalThreads; i++) {
            int id = i;
            try {
                pool.execute(() -> {
                    System.out.println("[Task] " + Thread.currentThread().getName() + " started task " + id);
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    System.out.println("[Task] " + Thread.currentThread().getName() + " finished task " + id);
                });
            } catch (RejectedExecutionException e) {
                System.out.println("Задача " + id + " была отклонена: " + e.getMessage());
            }
        }


        // Future submit
        try {
            Future<String> future = pool.submit(() -> {
                Thread.sleep(1000);
                return "Callable result";
            });
            System.out.println("[Main] Result from future: " + future.get());
        } catch (RejectedExecutionException e) {
            System.out.println("Submit был отклонён: " + e.getMessage());
        }

        // shutdown после задержки
        Thread.sleep(8000);
        pool.shutdown();

        // Пауза для graceful shutdown
        Thread.sleep(3000);
        pool.shutdownNow();

        System.out.println("[Main] Main finished.");
    }
}
