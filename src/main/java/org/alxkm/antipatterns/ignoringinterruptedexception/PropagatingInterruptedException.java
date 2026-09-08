package org.alxkm.antipatterns.ignoringinterruptedexception;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Another approach is to propagate the InterruptedException up the call stack, letting the caller
 * decide what cancellation means.
 * <p>
 * This is the better option whenever the method is allowed to declare it, because the caller keeps
 * both facts: that the work did not finish, and that the thread was asked to stop. Note that
 * {@link Runnable#run()} declares no checked exceptions, so a Runnable <em>cannot</em> propagate --
 * it is forced into {@link ProperlyHandlingInterruptedException}'s approach of restoring the flag.
 * Implementing {@link Callable} instead is what makes propagation available, and the executor then
 * delivers the exception to whoever calls {@link Future#get()}.
 */
public class PropagatingInterruptedException implements Callable<Void> {

    /**
     * Performs a long-running task, letting an interruption propagate to the caller.
     *
     * @return {@code null}; the task produces no value.
     * @throws InterruptedException if the thread is interrupted while working. It is deliberately
     *                              neither caught nor logged here.
     */
    @Override
    public Void call() throws InterruptedException {
        while (true) {
            // Simulating long-running task. Thread.sleep throws rather than returning early, so the
            // exception simply travels up and out of call().
            Thread.sleep(1000);
            System.out.println("Working...");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(new PropagatingInterruptedException());

        // Cancel the task after 3 seconds; cancel(true) interrupts the worker thread.
        Thread.sleep(3000);
        future.cancel(true);

        try {
            future.get();
        } catch (ExecutionException e) {
            // The task failed on its own terms and the cause arrives intact.
            System.out.println("Task failed: " + e.getCause());
        } catch (CancellationException e) {
            // cancel(true) won the race, which is the expected outcome here.
            System.out.println("Task was cancelled, and the interruption was not swallowed.");
        }

        executor.shutdown();
    }
}
