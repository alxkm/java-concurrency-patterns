package org.alxkm.antipatterns.forgottensynchronization;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ForgottenSynchronizationTest {

    /**
     * Demonstrates that CounterExample has race conditions due to forgotten synchronization
     */
    @RepeatedTest(10)
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testForgottenSynchronizationCausesRaceCondition() throws InterruptedException {
        CounterExample counter = new CounterExample();
        final int THREADS = 10;
        final int INCREMENTS_PER_THREAD = 1000;
        final int EXPECTED_TOTAL = THREADS * INCREMENTS_PER_THREAD;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREADS);
        
        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Ensure all threads start together
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        counter.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
        
        int actualCount = counter.getCounter();
        assertTrue(actualCount < EXPECTED_TOTAL, 
                   "Without synchronization, lost updates should occur. Expected less than " + 
                   EXPECTED_TOTAL + " but got " + actualCount);
    }

    /**
     * Demonstrates visibility issues with forgotten synchronization
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testVisibilityIssue() throws InterruptedException {
        CounterExample counter = new CounterExample();
        AtomicBoolean writerFinished = new AtomicBoolean(false);
        AtomicInteger visibleValue = new AtomicInteger(-1);
        
        // Writer thread
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
            writerFinished.set(true);
        });
        
        // Reader thread - may not see all updates due to lack of synchronization
        Thread reader = new Thread(() -> {
            while (!writerFinished.get()) {
                // Keep reading
            }
            // Even after writer finished, reader might not see the final value
            visibleValue.set(counter.getCounter());
        });
        
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
        
        // The reader might see a stale value
        int finalValue = counter.getCounter();
        int readerSawValue = visibleValue.get();
        
        // Due to visibility issues, reader might see a different value
        assertTrue(readerSawValue <= finalValue, 
                   "Reader should see at most the final value, but saw " + 
                   readerSawValue + " while final was " + finalValue);
    }

    /**
     * Demonstrates that concurrent reads during writes can see inconsistent states
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testInconsistentReads() throws InterruptedException {
        CounterExample counter = new CounterExample();
        Set<Integer> seenValues = new ConcurrentSkipListSet<>();
        AtomicBoolean stopReading = new AtomicBoolean(false);
        final int EXPECTED_MAX = 5000;
        
        // Reader thread that continuously reads values
        Thread reader = new Thread(() -> {
            while (!stopReading.get()) {
                int value = counter.getCounter();
                seenValues.add(value);
                if (value < 0 || value > EXPECTED_MAX) {
                    fail("Saw impossible value: " + value);
                }
            }
        });
        
        // Writer thread
        Thread writer = new Thread(() -> {
            for (int i = 0; i < EXPECTED_MAX; i++) {
                counter.increment();
            }
        });
        
        reader.start();
        writer.start();
        
        writer.join();
        stopReading.set(true);
        reader.join();
        
        // Check that we didn't see all values (some updates were missed)
        // This demonstrates the race condition
        assertTrue(seenValues.size() < EXPECTED_MAX, 
                   "Due to race conditions, reader should miss some values. Saw " + 
                   seenValues.size() + " different values out of " + EXPECTED_MAX);
    }

    /**
     * Test demonstrating compound operation issues with forgotten synchronization
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testCompoundOperationRaceCondition() throws InterruptedException {
        CounterExample counter = new CounterExample();
        final int THREADS = 10;
        final int INCREMENTS_PER_THREAD = 1000;
        final int EXPECTED_TOTAL = THREADS * INCREMENTS_PER_THREAD;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREADS);
        
        // Each thread performs increments without proper synchronization
        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        counter.increment(); // Race condition here
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
        
        int actualCount = counter.getCounter();
        
        // Due to race conditions, we should see lost updates
        assertTrue(actualCount < EXPECTED_TOTAL, 
                   "Race conditions should cause lost updates. Expected " + EXPECTED_TOTAL + 
                   " but got " + actualCount + ". Lost updates: " + (EXPECTED_TOTAL - actualCount));
        
        // Also verify we lost a significant number of updates (at least 1%)
        int lostUpdates = EXPECTED_TOTAL - actualCount;
        assertTrue(lostUpdates > EXPECTED_TOTAL * 0.01, 
                   "Should lose significant number of updates due to race conditions. Lost: " + 
                   lostUpdates + " out of " + EXPECTED_TOTAL);
    }
}