package org.alxkm.antipatterns.threadsinsteadoftasks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Another approach is to use CompletableFuture,
 * which is part of the java.util.concurrent package and allows for more flexible task management and asynchronous programming.
 * <p>
 * <p>
 * In this version, CompletableFuture.runAsync is used to execute tasks asynchronously.
 * CompletableFuture provides a rich API for composing and managing asynchronous tasks, making it easier to handle complex workflows.
 */
public class CompletableFutureExample {
    /**
     * A simple task that prints the thread name.
     */
    public void performTask() {
        CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> System.out.println("Task executed by: " + Thread.currentThread().getName()));
        CompletableFuture<Void> task2 = CompletableFuture.runAsync(() -> System.out.println("Task executed by: " + Thread.currentThread().getName()));

        // Wait for all tasks to complete
        try {
            CompletableFuture.allOf(task1, task2).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CompletableFutureExample manager = new CompletableFutureExample();
        manager.performTask();
    }
}
