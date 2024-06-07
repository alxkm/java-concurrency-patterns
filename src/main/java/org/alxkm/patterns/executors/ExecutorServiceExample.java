package org.alxkm.patterns.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The ExecutorServiceExample class demonstrates the usage of an ExecutorService
 * with a fixed thread pool size to execute multiple tasks concurrently.
 */
public class ExecutorServiceExample {
    /**
     * The main method creates an ExecutorService with a fixed thread pool size of 3
     * and submits tasks to it. Each task simulates some execution time and prints
     * its completion status along with the thread name.
     *
     * @param args The command-line arguments (unused).
     */
    public static void main(String[] args) {
        // Create an ExecutorService with a fixed thread pool size of 3
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        // Submit tasks to the ExecutorService
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executorService.submit(() -> {
                try {
                    // Simulate some task execution
                    System.out.println("Task " + taskId + " is running on thread: " + Thread.currentThread().getName());
                    Thread.sleep(2000);
                    System.out.println("Task " + taskId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Shutdown the ExecutorService
        executorService.shutdown();
    }
}