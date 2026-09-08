package org.alxkm.patterns.future;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The FutureExample class demonstrates the usage of the Future interface
 * to represent the result of an asynchronous computation.
 */
public class FutureExample {

    /**
     * The main method creates an ExecutorService with a single thread,
     * submits a task to the executor, and retrieves the result from the Future.
     * It then prints the result and shuts down the executor.
     *
     * @param args The command-line arguments (unused).
     * @throws InterruptedException if the current thread is interrupted while awaiting the result.
     * @throws ExecutionException   if the task completed with an exception.
     */
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Integer> future = executorService.submit(new Task());

        try {
            System.out.println("Result from future: " + future.get());
        } finally {
            executorService.shutdown();
        }
    }
}
