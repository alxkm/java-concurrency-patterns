package org.alxkm.antipatterns.racecondition;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class RaceConditionTest {

    /**
     * Demonstrates that unsafeIncrementAmount has a race condition
     */
    @RepeatedTest(10) // Run multiple times to increase chance of catching race condition
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testUnsafeIncrementHasRaceCondition() throws InterruptedException {
        AccountAmount account = new AccountAmount();
        final int THREADS = 10;
        final int INCREMENTS_PER_THREAD = 1000;
        final int EXPECTED_TOTAL = THREADS * INCREMENTS_PER_THREAD;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREADS);
        
        // Create threads that will all start incrementing at the same time
        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal to start
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        account.unsafeIncrementAmount();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads at the same time
        startLatch.countDown();
        
        // Wait for all threads to complete
        doneLatch.await();
        executor.shutdown();
        
        // The actual amount should be less than expected due to race condition
        int actualAmount = account.getAmount();
        assertTrue(actualAmount < EXPECTED_TOTAL, 
                   "Race condition should cause lost updates. Expected less than " + 
                   EXPECTED_TOTAL + " but got " + actualAmount);
    }

    /**
     * Verifies that synchronized increment is thread-safe
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testSafeSynchronizedIncrementIsThreadSafe() throws InterruptedException {
        AccountAmount account = new AccountAmount();
        final int THREADS = 10;
        final int INCREMENTS_PER_THREAD = 1000;
        final int EXPECTED_TOTAL = THREADS * INCREMENTS_PER_THREAD;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch doneLatch = new CountDownLatch(THREADS);
        
        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    account.safeSynchronizedIncrementAmount();
                }
                doneLatch.countDown();
            });
        }
        
        doneLatch.await();
        executor.shutdown();
        
        assertEquals(EXPECTED_TOTAL, account.getAmount(), 
                     "Synchronized increment should be thread-safe");
    }

    /**
     * Verifies that lock-based increment is thread-safe
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testSafeLockIncrementIsThreadSafe() throws InterruptedException {
        AccountAmount account = new AccountAmount();
        final int THREADS = 10;
        final int INCREMENTS_PER_THREAD = 1000;
        final int EXPECTED_TOTAL = THREADS * INCREMENTS_PER_THREAD;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch doneLatch = new CountDownLatch(THREADS);
        
        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    account.safeLockIncrementAmount();
                }
                doneLatch.countDown();
            });
        }
        
        doneLatch.await();
        executor.shutdown();
        
        assertEquals(EXPECTED_TOTAL, account.getAmount(), 
                     "Lock-based increment should be thread-safe");
    }

    /**
     * Verifies that atomic increment is thread-safe
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testAtomicIncrementIsThreadSafe() throws InterruptedException {
        AccountAmount account = new AccountAmount();
        final int THREADS = 10;
        final int INCREMENTS_PER_THREAD = 1000;
        final int EXPECTED_TOTAL = THREADS * INCREMENTS_PER_THREAD;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch doneLatch = new CountDownLatch(THREADS);
        
        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    account.incrementAtomicAmount();
                }
                doneLatch.countDown();
            });
        }
        
        doneLatch.await();
        executor.shutdown();
        
        assertEquals(EXPECTED_TOTAL, account.getAtomicInteger().get(), 
                     "Atomic increment should be thread-safe");
    }

    /**
     * Demonstrates race condition with multiple readers and writers
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testReadWriteRaceCondition() throws InterruptedException {
        AccountAmount account = new AccountAmount();
        AtomicInteger inconsistentReads = new AtomicInteger(0);
        final int WRITERS = 5;
        final int READERS = 5;
        final int OPERATIONS = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(WRITERS + READERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(WRITERS + READERS);
        
        // Start writers
        for (int i = 0; i < WRITERS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS; j++) {
                        account.unsafeIncrementAmount();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start readers that check for consistency
        for (int i = 0; i < READERS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int lastValue = -1;
                    for (int j = 0; j < OPERATIONS; j++) {
                        int currentValue = account.getAmount();
                        if (currentValue < lastValue) {
                            inconsistentReads.incrementAndGet();
                        }
                        lastValue = currentValue;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
        
        // We might see some reads where the value appears to go backwards
        // This is a sign of race conditions
        assertTrue(account.getAmount() < WRITERS * OPERATIONS, 
                   "Race condition should cause lost updates");
    }
}