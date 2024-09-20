package org.alxkm.patterns.executors;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the usage of ScheduledThreadPoolExecutor to schedule tasks.
 */
public class ScheduledThreadPoolExecutorExample {

    /**
     *
     * We create a ScheduledThreadPoolExecutor with 3 threads.
     * We schedule three different tasks:
     * Task 1: Executes after a delay of 1 second using schedule().
     * Task 2: Executes repeatedly with a fixed rate, starting after an initial delay of 2 seconds, with a period of 3 seconds using scheduleAtFixedRate().
     * Task 3: Executes repeatedly with a fixed delay between termination of one execution and commencement of the next, starting after an initial delay of 2 seconds, with a delay of 3 seconds using scheduleWithFixedDelay().
     * Finally, we schedule a task to shut down the executor after 10 seconds using schedule().
     *
     */
    public static void main(String[] args) {
        // Create a ScheduledThreadPoolExecutor with 3 threads
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);

        // Schedule a task to run after a delay of 1 second
        executor.schedule(() -> {
            System.out.println("Task 1 executed after 1 second.");
        }, 1, TimeUnit.SECONDS);

        // Schedule a task to run repeatedly with an initial delay of 2 seconds and a period of 3 seconds
        executor.scheduleAtFixedRate(() -> {
            System.out.println("Task 2 executed repeatedly with a fixed rate.");
        }, 2, 3, TimeUnit.SECONDS);

        // Schedule a task to run repeatedly with an initial delay of 2 seconds and a delay between termination of one execution and commencement of the next
        executor.scheduleWithFixedDelay(() -> {
            System.out.println("Task 3 executed repeatedly with a fixed delay.");
        }, 2, 3, TimeUnit.SECONDS);

        // Shutdown the executor after 10 seconds
        executor.schedule(() -> {
            System.out.println("Shutting down the executor...");
            executor.shutdown();
        }, 10, TimeUnit.SECONDS);
    }
}

