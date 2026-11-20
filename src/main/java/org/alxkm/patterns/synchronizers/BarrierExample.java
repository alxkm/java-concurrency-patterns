package org.alxkm.patterns.synchronizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CyclicBarrier: workers compute in rounds, and the barrier action merges each round.
 * <p>
 * Three things a barrier gives that a loop of threads does not:
 * <ul>
 *   <li><b>It is cyclic.</b> The same barrier trips once per round; nothing has to be rebuilt.</li>
 *   <li><b>The barrier action is the merge step.</b> It runs once per round, in the last thread to
 *   arrive, after every worker has written its part and before any worker starts the next round. The
 *   barrier also makes those writes visible: what a worker does before await() happens-before the
 *   action, and the action happens-before what every worker does after await() returns.</li>
 *   <li><b>All or none.</b> If one party is interrupted or times out, the barrier breaks and every
 *   other party gets BrokenBarrierException instead of waiting for it.</li>
 * </ul>
 * The last point has a catch. A worker that fails with an ordinary exception never reaches await(),
 * and the others wait for it forever. It has to break the barrier on its way out, and reset() is not
 * the way: reset() breaks it only for the parties already waiting, and a worker that arrives after the
 * reset waits on a fresh barrier for a party that will never come. See {@link #breakForEveryone}.
 */
public final class BarrierExample {

    private BarrierExample() {
    }

    /**
     * Sums each round of numbers on several workers. Each worker adds up its share of the round, every
     * workers-th number starting at its own index; the barrier action adds up the workers' sums.
     *
     * @param rounds  the numbers of each round.
     * @param workers how many workers, and so how many parties the barrier has.
     * @return the total of each round.
     * @throws IllegalStateException if a worker failed; the cause is the worker's exception. The other
     *                               workers stop instead of waiting for it.
     * @throws InterruptedException  if interrupted while waiting for the workers.
     */
    public static long[] totalsByRound(long[][] rounds, int workers) throws InterruptedException {
        long[] partials = new long[workers];
        long[] totals = new long[rounds.length];
        int[] round = {0};
        CyclicBarrier barrier = new CyclicBarrier(workers, () -> {
            // Runs alone: every worker is waiting at the barrier, so the partials are complete and stable.
            totals[round[0]] = Arrays.stream(partials).sum();
            round[0]++;
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();

        List<Thread> threads = new ArrayList<>();
        for (int w = 0; w < workers; w++) {
            int worker = w;
            Thread thread = new Thread(() -> {
                try {
                    for (long[] values : rounds) {
                        long sum = 0;
                        for (int i = worker; i < values.length; i += workers) {
                            sum = Math.addExact(sum, values[i]);
                        }
                        partials[worker] = sum;
                        barrier.await();
                    }
                } catch (BrokenBarrierException e) {
                    // Another worker failed and broke the barrier; it has recorded why.
                } catch (InterruptedException e) {
                    failure.compareAndSet(null, e);
                    Thread.currentThread().interrupt(); // await() has already broken the barrier
                } catch (RuntimeException e) {
                    failure.compareAndSet(null, e);
                    breakForEveryone(barrier);
                }
            }, "barrier-worker-" + w);
            thread.setDaemon(true); // a stuck barrier should not keep the JVM alive
            threads.add(thread);
        }

        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
        if (failure.get() != null) {
            throw new IllegalStateException("a worker failed, so no round after it was totalled", failure.get());
        }
        return totals;
    }

    /**
     * Breaks the barrier for every party: those waiting now and those that arrive later.
     * <p>
     * CyclicBarrier has no public method for this. An await() from a thread that is already interrupted
     * does it: the barrier breaks and stays broken, so later arrivals get BrokenBarrierException at once.
     * reset() would only break it for the parties waiting at that moment.
     *
     * @param barrier the barrier to break.
     */
    static void breakForEveryone(CyclicBarrier barrier) {
        Thread.currentThread().interrupt();
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException expected) {
            // InterruptedException: this call broke it. BrokenBarrierException: it was broken already.
        } finally {
            Thread.interrupted(); // clear the flag set above, whichever way await() returned
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long[][] rounds = {
                {1, 2, 3, 4, 5, 6},
                {10, 20, 30, 40, 50, 60},
                {100, 200, 300, 400, 500, 600},
        };
        System.out.println("Totals by round, 3 workers: " + Arrays.toString(totalsByRound(rounds, 3)));

        long[][] overflowing = {{1, 2, 3}, {Long.MAX_VALUE, 1, 0, Long.MAX_VALUE}};
        try {
            totalsByRound(overflowing, 2);
        } catch (IllegalStateException e) {
            System.out.println("One worker overflowed in round 1: " + e.getCause()
                    + "; the other got a broken barrier instead of waiting for it");
        }
    }
}
