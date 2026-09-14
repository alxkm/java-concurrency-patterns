package org.alxkm.patterns.executors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    /** Tasks submitted by each flavour, and the work each one simulates. */
    private static final int TASKS = 5;

    private static final long DEMO_WORK_MILLIS = 1000;

    public static void main(String[] args) {
        // Example 1: FixedThreadPool
        System.out.println("FixedThreadPool used " + fixedThreadPoolExample(DEMO_WORK_MILLIS) + " threads");

        // Example 2: CachedThreadPool
        System.out.println("CachedThreadPool used " + cachedThreadPoolExample(DEMO_WORK_MILLIS) + " threads");

        // Example 3: SingleThreadExecutor
        System.out.println("SingleThreadExecutor used " + singleThreadExecutorExample(DEMO_WORK_MILLIS) + " threads");
    }

    /**
     * Example of using FixedThreadPool.
     */
    /**
     * A fixed pool caps the thread count whatever arrives: 5 tasks, at most 3 threads.
     *
     * @param workMillis how long each task simulates working for.
     * @return how many distinct threads ran the tasks.
     */
    public static int fixedThreadPoolExample(long workMillis) {
        System.out.println("FixedThreadPool Example:");

        // Create a FixedThreadPool with 3 threads
        ExecutorService executor = Executors.newFixedThreadPool(3);

        Set<Long> threadIds = submitTasks(executor, workMillis);
        shutdownExecutor(executor);
        return threadIds.size();
    }

    /**
     * Example of using CachedThreadPool.
     */
    /**
     * A cached pool creates a thread per task when none is free, so it has no upper bound. Convenient,
     * and the reason it is the wrong default for untrusted load: the thread count follows the arrival
     * rate.
     *
     * @param workMillis how long each task simulates working for.
     * @return how many distinct threads ran the tasks.
     */
    public static int cachedThreadPoolExample(long workMillis) {
        System.out.println("\nCachedThreadPool Example:");

        // Create a CachedThreadPool
        ExecutorService executor = Executors.newCachedThreadPool();

        Set<Long> threadIds = submitTasks(executor, workMillis);
        shutdownExecutor(executor);
        return threadIds.size();
    }

    /**
     * Example of using SingleThreadExecutor.
     */
    /**
     * A single-thread executor runs everything on one thread, in submission order. That ordering is
     * the reason to pick it: it serialises work without you writing a lock.
     *
     * @param workMillis how long each task simulates working for.
     * @return how many distinct threads ran the tasks, which is always 1.
     */
    public static int singleThreadExecutorExample(long workMillis) {
        System.out.println("\nSingleThreadExecutor Example:");

        // Create a SingleThreadExecutor
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Set<Long> threadIds = submitTasks(executor, workMillis);
        shutdownExecutor(executor);
        return threadIds.size();
    }

    /**
     * Submit example tasks to the given executor.
     *
     * @param executor the executor to submit tasks to
     */
    /**
     * Submits the same batch of work to whichever executor it is given.
     *
     * @param executor   the executor under demonstration.
     * @param workMillis how long each task simulates working for.
     * @return the ids of the threads that ran the tasks; read it after the executor has drained.
     */
    private static Set<Long> submitTasks(ExecutorService executor, long workMillis) {
        Set<Long> threadIds = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < TASKS; i++) {
            final int taskId = i;
            executor.submit(() -> {
                threadIds.add(Thread.currentThread().threadId());
                System.out.println("Task " + taskId + " executed by thread: " + Thread.currentThread().getName());
                try {
                    Thread.sleep(workMillis); // Simulate task execution time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        return threadIds;
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
            // Interrupted while waiting: cancel the stragglers and hand the interrupt on, rather
            // than returning as though the pool had drained.
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

