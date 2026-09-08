package org.alxkm.antipatterns.usingthreadsafecollectionsincorrectly;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Runs each {@link BaseListUsage} implementation under contention and reports whether the
 * check-then-add stayed atomic.
 * <p>
 * The two workers deliberately overlap on half their range, so a correct implementation ends up
 * with exactly {@link #EXPECTED_UNIQUE} elements. Anything above that count is a duplicate, which
 * can only mean two threads passed the "is it absent?" check for the same element before either of
 * them added it.
 */
public class UsageExample {
    private static final int THREADS = 8;
    private static final int RANGE = 500;
    private static final int EXPECTED_UNIQUE = RANGE;

    /**
     * Exercises one implementation from {@link #THREADS} contending threads and prints the outcome.
     *
     * @param usage the implementation under test.
     * @param <T>   the element type of the backing collection.
     * @throws InterruptedException if the current thread is interrupted while awaiting the workers.
     */
    public static <T> void runExample(BaseListUsage<T> usage) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);

        // Every thread offers the identical range, so all THREADS of them contend on each element.
        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = worker(usage, startGate);
            workers[i].start();
        }
        startGate.countDown();
        for (Thread worker : workers) {
            worker.join();
        }

        Set<T> distinct = new HashSet<>(usage.getCollection());
        int duplicates = usage.size() - distinct.size();

        System.out.printf("%-15s size=%d (expected %d), duplicates=%d -> %s%n",
                usage.getClass().getSimpleName(),
                usage.size(),
                EXPECTED_UNIQUE,
                duplicates,
                duplicates == 0 ? "atomic" : "check-then-add raced");
    }

    /**
     * Builds a worker that offers all {@link #RANGE} elements, releasing only once the start gate
     * opens so that the threads contend rather than run back to back.
     */
    private static <T> Thread worker(BaseListUsage<T> usage, CountDownLatch startGate) {
        return new Thread(() -> {
            try {
                startGate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            for (int i = 0; i < RANGE; i++) {
                usage.addIfAbsent("Element " + i);
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        runExample(new IncorrectUsage());
        runExample(new CorrectUsage());
        runExample(new OptimizedUsage());
    }
}
