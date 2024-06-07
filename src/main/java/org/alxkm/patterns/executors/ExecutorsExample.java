package org.alxkm.patterns.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the usage of Executors utility class to create thread pools.
 */
public class ExecutorsExample {

    /**
     * We demonstrate the usage of the Executors utility class to create three different types of thread pools: FixedThreadPool, CachedThreadPool, and SingleThreadExecutor.
     * For each type of thread pool, we submit 5 example tasks to the executor using the submit() method.
     * Each task prints its ID and the name of the thread executing it, simulating a task execution time of 1 second.
     * Finally, we shut down each executor gracefully using the shutdown() method, waiting for all tasks to complete or for a maximum of 5 seconds before forcing shutdown using the shutdownNow() method.
     */
    public static void main(String[] args) {
        // Example 1: FixedThreadPool
        fixedThreadPoolExample();

        // Example 2: CachedThreadPool
        cachedThreadPoolExample();

        // Example 3: SingleThreadExecutor
        singleThreadExecutorExample();
    }

    /**
     * Example of using FixedThreadPool.
     */
    private static void fixedThreadPoolExample() {
        System.out.println("FixedThreadPool Example:");

        // Create a FixedThreadPool with 3 threads
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Submit tasks to the executor
        submitTasks(executor);

        // Shutdown the executor
        shutdownExecutor(executor);
    }

    /**
     * Example of using CachedThreadPool.
     */
    private static void cachedThreadPoolExample() {
        System.out.println("\nCachedThreadPool Example:");

        // Create a CachedThreadPool
        ExecutorService executor = Executors.newCachedThreadPool();

        // Submit tasks to the executor
        submitTasks(executor);

        // Shutdown the executor
        shutdownExecutor(executor);
    }

    /**
     * Example of using SingleThreadExecutor.
     */
    private static void singleThreadExecutorExample() {
        System.out.println("\nSingleThreadExecutor Example:");

        // Create a SingleThreadExecutor
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Submit tasks to the executor
        submitTasks(executor);

        // Shutdown the executor
        shutdownExecutor(executor);
    }

    /**
     * Submit example tasks to the given executor.
     *
     * @param executor the executor to submit tasks to
     */
    private static void submitTasks(ExecutorService executor) {
        // Submit tasks to the executor
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by thread: " + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000); // Simulate task execution time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Shutdown the given executor.
     *
     * @param executor the executor to shutdown
     */
    private static void shutdownExecutor(ExecutorService executor) {
        // Shutdown the executor
        executor.shutdown();
        try {
            // Wait for all tasks to complete or for 5 seconds, whichever comes first
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                // If not all tasks completed within 5 seconds, force shutdown
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Handle interruption
            e.printStackTrace();
        }
    }
}

