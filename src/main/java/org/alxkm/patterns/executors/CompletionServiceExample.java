package org.alxkm.patterns.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Demonstrates the usage of ExecutorCompletionService for managing and retrieving
 * the results of asynchronous tasks.
 */
public class CompletionServiceExample {
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final CompletionService<String> completionService = new ExecutorCompletionService<>(executorService);

    /**
     * Submits a list of callable tasks to the executor and processes their results
     * as they complete.
     *
     * @param tasks A list of Callable tasks to be executed.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     * @throws ExecutionException   If a task computation threw an exception.
     */
    public void executeTasks(List<Callable<String>> tasks) throws InterruptedException, ExecutionException {
        // Submit all tasks
        for (Callable<String> task : tasks) {
            completionService.submit(task);
        }

        // Retrieve and print the results as they complete
        for (int i = 0; i < tasks.size(); i++) {
            Future<String> future = completionService.take(); // Blocks until a result is available
            System.out.println("Result: " + future.get());
        }

        shutdown();
    }

    /**
     * Shuts down the executor service.
     */
    private void shutdown() {
        executorService.shutdown();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        CompletionServiceExample example = new CompletionServiceExample();

        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep((int) (Math.random() * 1000));
                return "Task " + taskId + " completed";
            });
        }

        example.executeTasks(tasks);
    }
}
