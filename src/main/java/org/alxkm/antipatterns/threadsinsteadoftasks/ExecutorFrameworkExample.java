package org.alxkm.antipatterns.threadsinsteadoftasks;

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
    /**
     * A simple task that prints the thread name.
     *
     * @throws InterruptedException if the current thread is interrupted while awaiting termination.
     */
    public void performTask() throws InterruptedException {
        Runnable task = () -> {
            System.out.println("Task executed by: " + Thread.currentThread().getName());
        };

        // Using ExecutorService to manage threads
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(task);
        executor.submit(task);

        // Shutdown the executor and wait for tasks to finish. awaitTermination returns whether the
        // pool actually drained in time; ignoring that answer is how a "graceful" shutdown quietly
        // abandons still-running tasks, so escalate to shutdownNow() when it reports false.
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorFrameworkExample manager = new ExecutorFrameworkExample();
        manager.performTask();
    }
}
