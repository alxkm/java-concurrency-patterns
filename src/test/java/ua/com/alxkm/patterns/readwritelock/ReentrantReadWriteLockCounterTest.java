package ua.com.alxkm.patterns.readwritelock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReentrantReadWriteLockCounterTest {

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
