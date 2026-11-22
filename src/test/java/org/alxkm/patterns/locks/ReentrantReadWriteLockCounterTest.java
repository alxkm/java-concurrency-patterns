package org.alxkm.patterns.locks;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReentrantReadWriteLockCounterTest {

    /**
     * This test method verifies the concurrency behavior of the ReentrantReadWriteLockCounter class,
     * which implements a counter with thread-safe operations using ReentrantReadWriteLock. It creates
     * multiple threads (10 in this case), each incrementing the counter by a fixed amount (1000 in this
     * case). After all threads have completed their increments, the test checks whether the final value
     * of the counter matches the expected value, which is the total number of increments performed by
     * all threads combined.
     */
    @Test
    void testCounterConcurrency() throws InterruptedException {
        final ReentrantReadWriteLockCounter counter = new ReentrantReadWriteLockCounter();
        final int numThreads = 10;
        final int incrementsPerThread = 1000;

        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.incrementCounter();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        final int expectedValue = numThreads * incrementsPerThread;
        final int counterValue = counter.getCounter();
        assertEquals(expectedValue, counterValue, "The counter value should be equal to the expected value");
    }
}
