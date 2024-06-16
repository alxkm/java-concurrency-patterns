package org.alxkm.patterns.synchronizers;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BarrierTest {

    /**
     * This test method verifies the behavior of the Barrier class, which implements a simple barrier
     * synchronization mechanism. It creates two threads, t1 and t2, both of which await on the barrier
     * before proceeding. The barrier is initialized with a count of 2, indicating that it should block
     * until two threads have arrived. Once both threads arrive at the barrier, the test ensures that the
     * latch count is decremented to 0, indicating that both threads have successfully passed through the barrier.
     */
    @Test
    public void testBarrier() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Barrier barrier = new Barrier(2, latch::countDown);

        Thread t1 = new Thread(() -> {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertEquals(0, latch.getCount());
    }
}
