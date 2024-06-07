package org.alxkm.patterns.readwritelock;

import org.junit.jupiter.api.Test;
import org.alxkm.patterns.locks.ReadWriteLockExample;
import org.alxkm.patterns.locks.ReentrantReadWriteLockCounter;

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

    /**
     * This test method verifies the functionality of the ReadWriteLockExample class, which demonstrates
     * the usage of ReadWriteLock to provide concurrent read access and exclusive write access to a shared
     * resource. It sets a value using the write method and then reads the value using the read method.
     * The test asserts that the value read from the shared resource matches the value previously written,
     * confirming that the read and write operations are performed correctly with the appropriate locking
     * mechanisms in place to ensure thread safety.
     */

    @Test
    public void testReadWriteLock() throws InterruptedException {
        ReadWriteLockExample example = new ReadWriteLockExample();
        example.write(42);
        assertEquals(42, example.read());
    }
}
