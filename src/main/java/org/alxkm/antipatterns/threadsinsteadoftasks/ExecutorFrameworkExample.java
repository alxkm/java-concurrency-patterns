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
     */
    public void performTask() {
        Runnable task = () -> {
            System.out.println("Task executed by: " + Thread.currentThread().getName());
        };

        // Using ExecutorService to manage threads
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(task);
        executor.submit(task);

        // Shutdown the executor and wait for tasks to finish
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ExecutorFrameworkExample manager = new ExecutorFrameworkExample();
        manager.performTask();
    }
}
