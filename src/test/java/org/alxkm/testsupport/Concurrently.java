package org.alxkm.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs an action from several threads that all start at the same moment.
 * <p>
 * Starting threads in a loop and letting them run as they are created is the usual way a
 * concurrency test ends up proving nothing: the first thread often finishes its whole workload
 * before the last one begins, so the operations never overlap and a broken implementation passes.
 * A start gate holds every thread until all of them are ready, which is what actually produces
 * contention.
 */
public final class Concurrently {
    private static final long TIMEOUT_SECONDS = 30;

    private Concurrently() {
    }

    /**
     * Invokes {@code action} {@code threads * iterations} times, spread over that many threads.
     *
     * @param threads    how many threads to run.
     * @param iterations how many times each thread invokes the action.
     * @param action     the action to invoke; must be safe to call from many threads.
     * @throws InterruptedException if the calling thread is interrupted while waiting.
     */
    public static void run(int threads, int iterations, Runnable action) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            for (int i = 0; i < threads; i++) {
                executor.execute(() -> {
                    try {
                        startGate.await();
                        for (int j = 0; j < iterations; j++) {
                            action.run();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }

            startGate.countDown();
            assertTrue(finished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "workers did not finish in time");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "executor did not terminate");
        }
    }

    /**
     * Calls {@code supplier} once from each of {@code threads} threads, all released together, and
     * returns what each call produced.
     * <p>
     * Useful for lazy-initialisation tests, where the question is whether every caller received the
     * same instance when they all arrived at once.
     *
     * @param threads  how many threads to run.
     * @param supplier the value to obtain on each thread.
     * @param <T>      the produced type.
     * @return one result per thread, in submission order.
     * @throws InterruptedException if the calling thread is interrupted while waiting.
     * @throws ExecutionException   if any invocation threw.
     */
    public static <T> List<T> collect(int threads, Callable<T> supplier)
            throws InterruptedException, ExecutionException {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<T>> futures = new ArrayList<>(threads);

        try {
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    startGate.await();
                    return supplier.call();
                }));
            }
            startGate.countDown();

            List<T> results = new ArrayList<>(threads);
            for (Future<T> future : futures) {
                results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } catch (java.util.concurrent.TimeoutException e) {
            throw new AssertionError("threads did not finish within " + TIMEOUT_SECONDS + "s", e);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "executor did not terminate");
        }
    }
}
