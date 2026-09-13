package org.alxkm.antipatterns.threadsinsteadoftasks;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
     *
     * @throws InterruptedException if the current thread is interrupted while awaiting the tasks.
     * @throws ExecutionException   if either task completed with an exception.
     */
    public int performTask() throws InterruptedException, ExecutionException {
        Set<Long> threadIds = ConcurrentHashMap.newKeySet();
        Runnable task = () -> {
            threadIds.add(Thread.currentThread().threadId());
            System.out.println("Task executed by: " + Thread.currentThread().getName());
        };

        CompletableFuture<Void> task1 = CompletableFuture.runAsync(task);
        CompletableFuture<Void> task2 = CompletableFuture.runAsync(task);

        // Wait for all tasks to complete
        CompletableFuture.allOf(task1, task2).get();
        return threadIds.size();
    }

    /**
     * Composes two dependent stages, which is what CompletableFuture adds over a bare executor.
     *
     * The second stage does not exist as a task until the first has produced a value, so no thread
     * waits for another: the chain is assembled up front and each step runs when its input arrives.
     *
     * @return the composed result.
     * @throws InterruptedException if the current thread is interrupted while awaiting the chain.
     * @throws ExecutionException   if any stage completed with an exception.
     */
    public String composeStages() throws InterruptedException, ExecutionException {
        return CompletableFuture.supplyAsync(() -> "first")
                .thenApplyAsync(value -> value + " -> second")
                .thenApplyAsync(value -> value + " -> third")
                .get();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        CompletableFutureExample manager = new CompletableFutureExample();
        System.out.println("Threads used: " + manager.performTask());
        System.out.println("Composed: " + manager.composeStages());
    }
}
