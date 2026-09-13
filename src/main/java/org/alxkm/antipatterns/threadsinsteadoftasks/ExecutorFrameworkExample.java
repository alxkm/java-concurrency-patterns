package org.alxkm.antipatterns.threadsinsteadoftasks;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * To resolve these issues, we can use the Executor framework, which provides a higher-level API for managing threads.
 * <p>
 * <p>
 * In this revised example, tasks are submitted to an ExecutorService with a fixed thread pool.
 * The Executor framework handles thread creation, management,
 * and resource cleanup, providing better scalability and error handling.
 */
public class ExecutorFrameworkExample {
    /** Threads in the pool, however many tasks arrive. */
    public static final int POOL_SIZE = 2;

    /** Task count used by the demo in main. */
    private static final int DEMO_TASKS = 2;

    /**
     * Runs two tasks on a fixed pool.
     *
     * @return how many distinct threads ran the tasks.
     * @throws InterruptedException if the current thread is interrupted while awaiting termination.
     */
    public int performTask() throws InterruptedException {
        return performTask(DEMO_TASKS);
    }

    /**
     * Runs the given number of tasks on a fixed pool of {@value #POOL_SIZE} threads.
     *
     * The difference from {@link DirectThreadManagement} is what happens as the work grows: the
     * thread count here does not move. Submitting ten times the tasks reuses the same threads
     * instead of creating ten times as many.
     *
     * @param taskCount how many tasks to submit.
     * @return how many distinct threads ran the tasks, never more than {@value #POOL_SIZE}.
     * @throws InterruptedException if the current thread is interrupted while awaiting termination.
     */
    public int performTask(int taskCount) throws InterruptedException {
        Set<Long> threadIds = ConcurrentHashMap.newKeySet();
        Runnable task = () -> {
            threadIds.add(Thread.currentThread().threadId());
            System.out.println("Task executed by: " + Thread.currentThread().getName());
        };

        // Using ExecutorService to manage threads
        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        for (int i = 0; i < taskCount; i++) {
            executor.submit(task);
        }

        // Shutdown the executor and wait for tasks to finish. awaitTermination returns whether the
        // pool actually drained in time; ignoring that answer is how a "graceful" shutdown quietly
        // abandons still-running tasks, so escalate to shutdownNow() when it reports false.
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
            executor.shutdownNow();
        }
        return threadIds.size();
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorFrameworkExample manager = new ExecutorFrameworkExample();
        System.out.println("Threads used: " + manager.performTask());
    }
}
