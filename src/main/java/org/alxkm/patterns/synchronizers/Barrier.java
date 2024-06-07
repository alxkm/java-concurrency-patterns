package org.alxkm.patterns.synchronizers;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * The Barrier class provides a synchronization mechanism that allows threads to synchronize at a certain
 * point to ensure all threads reach that point before proceeding. It encapsulates a CyclicBarrier instance
 * and provides a method for threads to await the barrier.
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
