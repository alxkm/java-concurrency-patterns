package org.alxkm.patterns.future;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Future: the handle to a task's result, and the three ways get() can fail.
 * <ul>
 *   <li><b>ExecutionException</b>: the task threw. getCause() is the very exception the task threw;
 *   FutureTask keeps the original. (A ForkJoinTask may rethrow a copy instead; see
 *   {@link org.alxkm.patterns.forkjoinpool.ForkJoinPoolExample}.)</li>
 *   <li><b>TimeoutException</b> from get(timeout): only the wait is over. The task keeps running and keeps
 *   its pool thread, and nothing will ever read its result. A caller that gives up on the result should
 *   cancel the task: {@link #getOrCancel}.</li>
 *   <li><b>CancellationException</b>: the future was cancelled. From then on get() throws this, even if
 *   the task goes on to finish: its result is thrown away.</li>
 * </ul>
 * cancel(true) interrupts the thread running the task; cancel(false) only keeps a task that has not
 * started from starting. Either way cancel() is a request. A running task stops only if it reacts to the
 * interrupt: a blocking call that throws InterruptedException, or a loop that checks for it. A task that
 * does neither runs to the end, and the future still reports it as cancelled.
 */
public final class FutureExample {

    private FutureExample() {
    }

    /**
     * Waits for the result, and cancels the task if the wait ends without one.
     *
     * @param future  the task's future.
     * @param timeout how long to wait.
     * @param unit    the unit of {@code timeout}.
     * @param <T>     the result type.
     * @return the result.
     * @throws ExecutionException   if the task threw; the cause is what it threw.
     * @throws TimeoutException     if the timeout passed. The task has been cancelled with an interrupt.
     * @throws InterruptedException if this thread was interrupted while waiting. The task has been
     *                              cancelled too: nobody is left to read its result.
     */
    public static <T> T getOrCancel(Future<T> future, long timeout, TimeUnit unit)
            throws ExecutionException, TimeoutException, InterruptedException {
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException | InterruptedException e) {
            future.cancel(true);
            throw e;
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "future-example");
            thread.setDaemon(true);
            return thread;
        });
        try {
            CountDownLatch interrupted = new CountDownLatch(1);
            Future<String> slow = executor.submit(() -> {
                try {
                    TimeUnit.SECONDS.sleep(30);
                    return "done";
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    throw e;
                }
            });
            try {
                getOrCancel(slow, 100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.out.println("get() timed out; cancelled: " + slow.isCancelled()
                        + "; the task saw the interrupt: " + interrupted.await(5, TimeUnit.SECONDS));
            }
            try {
                slow.get();
            } catch (CancellationException e) {
                System.out.println("get() after cancel: " + e.getClass().getSimpleName());
            }

            IllegalStateException thrownByTask = new IllegalStateException("no such order");
            Future<String> failing = executor.submit(() -> {
                throw thrownByTask;
            });
            try {
                failing.get();
            } catch (ExecutionException e) {
                System.out.println("get() on a failed task: ExecutionException caused by " + e.getCause()
                        + "; the same object the task threw: " + (e.getCause() == thrownByTask));
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
