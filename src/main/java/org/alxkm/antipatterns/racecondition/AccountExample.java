package org.alxkm.antipatterns.racecondition;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

/**
 * A class demonstrating different methods of incrementing an account amount with various
 * synchronization techniques.
 */
public class AccountExample {
    private static final int THREADS = 4;
    private static final int INCREMENTS_PER_THREAD = 100_000;

    /**
     * The main method demonstrating different methods of incrementing an account amount.
     *
     * @param args The command-line arguments (unused).
     */
    public static void main(String[] args) throws InterruptedException {
        int expected = THREADS * INCREMENTS_PER_THREAD;
        System.out.println("Expected total: " + expected);

        report("Unsafe", AccountAmount::unsafeIncrementAmount, AccountAmount::getUnsafeAmount, expected);
        report("Synchronized", AccountAmount::safeSynchronizedIncrementAmount, AccountAmount::getSynchronizedAmount, expected);
        report("ReentrantLock", AccountAmount::safeLockIncrementAmount, AccountAmount::getLockAmount, expected);
        report("AtomicInteger", AccountAmount::incrementAtomicAmount, AccountAmount::getAtomicAmount, expected);
    }

    /**
     * Runs one increment strategy on a fresh account from several threads and prints the result
     * alongside how many increments, if any, were lost.
     *
     * @param label     The name of the strategy being demonstrated.
     * @param increment The increment operation to invoke concurrently.
     * @param read      The matching accessor for the value the operation updates.
     * @param expected  The total that would be reached if no increment were lost.
     * @throws InterruptedException if the current thread is interrupted while awaiting the workers.
     */
    private static void report(String label,
                               Consumer<AccountAmount> increment,
                               ToIntFunction<AccountAmount> read,
                               int expected) throws InterruptedException {
        int actual = runConcurrently(increment, read);
        int lost = expected - actual;
        System.out.printf("%-14s = %,d (lost %,d)%n", label, actual, lost);
    }

    /**
     * Applies the given increment {@code THREADS * INCREMENTS_PER_THREAD} times across
     * {@link #THREADS} threads, then reads the result once every thread has terminated.
     * <p>
     * A {@link CountDownLatch} releases all workers at once so that they genuinely contend, rather
     * than the first thread finishing before the last one has started. Joining every thread before
     * reading establishes the happens-before edge that makes even the unsynchronized read
     * meaningful.
     *
     * @param increment The increment operation to invoke concurrently.
     * @param read      The matching accessor for the value the operation updates.
     * @return The value observed after all worker threads have terminated.
     * @throws InterruptedException if the current thread is interrupted while awaiting the workers.
     */
    private static int runConcurrently(Consumer<AccountAmount> increment,
                                       ToIntFunction<AccountAmount> read) throws InterruptedException {
        AccountAmount accountAmount = new AccountAmount();
        CountDownLatch startGate = new CountDownLatch(1);

        Thread[] workers = IntStream.range(0, THREADS)
                .mapToObj(i -> new Thread(() -> {
                    try {
                        startGate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        increment.accept(accountAmount);
                    }
                }))
                .toArray(Thread[]::new);

        for (Thread worker : workers) {
            worker.start();
        }
        startGate.countDown();
        for (Thread worker : workers) {
            worker.join();
        }

        return read.applyAsInt(accountAmount);
    }
}
