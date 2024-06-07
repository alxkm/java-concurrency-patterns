package org.alxkm.patterns.future;

import java.util.concurrent.Callable;
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
     */
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Integer> future = executorService.submit(new Task());

        try {
            System.out.println("Result from future: " + future.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }
}

/**
 * The Task class implements the Callable interface and represents a task
 * that computes a result asynchronously.
 */
class Task implements Callable<Integer> {

    /**
     * The call method is called by the ExecutorService to execute the task.
     * It returns the result of the computation.
     *
     * @return The result of the computation.
     * @throws Exception If an exception occurs during the computation.
     */
    @Override
    public Integer call() throws Exception {
        return 123;
    }
}

