package org.alxkm.patterns.executors;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the usage of ThreadPoolExecutor to execute tasks.
 */
public class ThreadPoolExecutorExample {
    /**
     * We create a ThreadPoolExecutor with a core pool size of 2, maximum pool size of 4, and a queue capacity of 10.
     * We submit 10 tasks to the executor using the submit() method.
     * Each task prints its ID and the name of the thread executing it, simulating a task execution time of 1 second.
     * Finally, we shut down the executor using the shutdown() method.
     */
    public static void main(String[] args) {
        // Create a ThreadPoolExecutor with a core pool size of 2, maximum pool size of 4, and a queue capacity of 10
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 0L, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>(10));

        try {
            // Submit tasks to the executor
            for (int i = 0; i < 10; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " executed by thread: " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(1000); // Simulate task execution time
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Task " + taskId + " was interrupted");
                    }
                });
            }
        } finally {
            // Shutdown the executor
            executor.shutdown();
            try {
                // Wait for tasks to complete
                if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }
}
