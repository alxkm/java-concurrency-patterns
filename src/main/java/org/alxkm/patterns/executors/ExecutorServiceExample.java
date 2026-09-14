package org.alxkm.patterns.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    /** Pool size, task count and simulated work used by the demo in main. */
    private static final int DEMO_POOL_SIZE = 3;

    private static final int DEMO_TASKS = 5;

    private static final long DEMO_WORK_MILLIS = 2000;

    public static void main(String[] args) {
        int completed = run(DEMO_POOL_SIZE, DEMO_TASKS, DEMO_WORK_MILLIS);
        System.out.println("Completed " + completed + " of " + DEMO_TASKS + " tasks.");
    }

    /**
     * Submits the tasks, then shuts the pool down and waits for them.
     *
     * shutdown() only stops new submissions; the tasks already accepted keep running. Returning
     * without awaitTermination is how a "graceful" shutdown quietly abandons work in progress, and
     * the boolean it returns is the only thing that tells you whether the pool actually drained.
     *
     * @param poolSize   how many threads the pool keeps.
     * @param taskCount  how many tasks to submit.
     * @param workMillis how long each task simulates working for.
     * @return how many tasks completed.
     */
    public static int run(int poolSize, int taskCount, long workMillis) {
        AtomicInteger completed = new AtomicInteger();

        // Create an ExecutorService with a fixed thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        try {
            // Submit tasks to the ExecutorService
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                executorService.submit(() -> {
                    try {
                        // Simulate some task execution
                        System.out.println("Task " + taskId + " is running on thread: " + Thread.currentThread().getName());
                        Thread.sleep(workMillis);
                        completed.incrementAndGet();
                        System.out.println("Task " + taskId + " completed");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Task " + taskId + " was interrupted");
                    }
                });
            }
        } finally {
            // Shutdown the ExecutorService
            executorService.shutdown();
            try {
                // Wait for tasks to complete
                if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate in the specified time.");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }
        return completed.get();
    }
}