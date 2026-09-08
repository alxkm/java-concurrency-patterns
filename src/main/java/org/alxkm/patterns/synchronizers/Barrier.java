package org.alxkm.patterns.synchronizers;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * A minimal facade over {@link CyclicBarrier}, holding threads at a rendezvous point until all
 * parties arrive and then running an optional barrier action.
 * <p>
 * This class adds no behaviour of its own; it exists to name the barrier concept and to keep the
 * example's call sites readable. Reach for {@link CyclicBarrier} directly in real code -- it also
 * offers timed waits, {@code getNumberWaiting()} and {@code reset()}, none of which are exposed here.
 * For a barrier built from first principles, see the synchronizer examples under
 * {@code org.alxkm.patterns.locks}.
 */
public class Barrier {
    private final CyclicBarrier barrier;

    /**
     * Constructs a Barrier with the specified number of threads and barrier action.
     *
     * @param numThreads    The number of threads to synchronize at the barrier.
     * @param barrierAction The action to be executed when all threads reach the barrier.
     */
    public Barrier(int numThreads, Runnable barrierAction) {
        barrier = new CyclicBarrier(numThreads, barrierAction);
    }

    /**
     * Waits until all parties have invoked await on this barrier.
     *
     * @throws InterruptedException   If the current thread is interrupted while waiting.
     * @throws BrokenBarrierException If the barrier is reset while any thread is waiting.
     */
    public void await() throws InterruptedException, BrokenBarrierException {
        barrier.await();
    }
}
