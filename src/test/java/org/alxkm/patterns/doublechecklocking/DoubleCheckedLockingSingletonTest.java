package org.alxkm.patterns.doublechecklocking;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DoubleCheckedLockingSingletonTest {

    private static final int THREAD_COUNT = 1000;

    /**
     * This test method verifies the thread safety of the DoubleCheckedLockingSingleton class
     * in a highly concurrent multithreaded environment. It creates a large number of threads (THREAD_COUNT)
     * that concurrently attempt to retrieve an instance of DoubleCheckedLockingSingleton using the getInstance
     * method. The test ensures that all instances obtained by different threads are the same, verifying the
     * correctness of the double-checked locking mechanism in ensuring thread safety and preserving the singleton
     * property of the class under high concurrency.
     */
    @Test
    public void testSingletonMultithreaded() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        // A start gate makes every thread reach getInstance() at once. Without it the pool ramps up
        // gradually and the first thread has usually published the instance before the last one
        // starts, so the initialisation race the test exists to stress never happens.
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(THREAD_COUNT);
        DoubleCheckedLockingSingleton[] instances = new DoubleCheckedLockingSingleton[THREAD_COUNT];

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                int index = i;
                executor.execute(() -> {
                    try {
                        startGate.await();
                        instances[index] = DoubleCheckedLockingSingleton.getInstance();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }

            startGate.countDown();
            // Bounded, so a hang fails the build with a clear message instead of waiting forever.
            assertTrue(finished.await(30, TimeUnit.SECONDS), "threads did not finish in time");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "executor did not terminate");
        }

        for (int i = 1; i < THREAD_COUNT; i++) {
            assertSame(instances[0], instances[i], "All instances should be the same");
        }
    }

    /**
     * This test method verifies the correctness of the DoubleCheckedLockingSingleton class,
     * ensuring that the getInstance method returns the same instance when called multiple times.
     * It obtains two instances of DoubleCheckedLockingSingleton using the getInstance method and
     * asserts that both instances are the same, confirming the singleton property of the class.
     */
    @Test
    public void testDoubleCheckedLockingSingleton() {
        DoubleCheckedLockingSingleton instance1 = DoubleCheckedLockingSingleton.getInstance();
        DoubleCheckedLockingSingleton instance2 = DoubleCheckedLockingSingleton.getInstance();

        assertSame(instance1, instance2);
    }
}