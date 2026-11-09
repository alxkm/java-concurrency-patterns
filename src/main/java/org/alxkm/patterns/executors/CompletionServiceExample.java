package org.alxkm.patterns.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ExecutorCompletionService: take results in the order the tasks finish, not the order they were
 * submitted.
 * <p>
 * Looping over a list of futures and calling get() on each waits for them in submission order, so one
 * slow task at the front holds up every result behind it, even results that are long done. A
 * CompletionService puts each future on a queue as its task completes, and take() hands back whichever
 * finished first.
 */
public class CompletionServiceExample {
    private static final int POOL_SIZE = 10;

    private CompletionServiceExample() {
    }

    /**
     * Runs the tasks and passes each result to the handler as soon as its task finishes. The handler
     * runs on the calling thread, one result at a time.
     *
     * @param tasks   the tasks to run.
     * @param handler receives each result, in completion order.
     * @throws InterruptedException if this thread is interrupted while waiting for a result.
     * @throws ExecutionException   if a task fails. The tasks still running are interrupted.
     */
    public static void executeTasks(List<Callable<String>> tasks, Consumer<String> handler)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        try {
            CompletionService<String> completionService = new ExecutorCompletionService<>(executor);
            for (Callable<String> task : tasks) {
                completionService.submit(task);
            }
            for (int i = 0; i < tasks.size(); i++) {
                // take() blocks until some task has finished, whichever one that is.
                handler.accept(completionService.take().get());
            }
        } finally {
            // All tasks are done on the normal path. After a failure, this stops the ones still running.
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Submitted as task 0 to 4, each one faster than the one before, so they finish in reverse.
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            final long workMillis = (5 - i) * 100L;
            tasks.add(() -> {
                Thread.sleep(workMillis);
                return "Task " + taskId + " completed after " + workMillis + " ms";
            });
        }

        executeTasks(tasks, result -> System.out.println("Result: " + result));
    }
}
