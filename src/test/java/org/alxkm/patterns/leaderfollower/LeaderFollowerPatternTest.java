package org.alxkm.patterns.leaderfollower;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class LeaderFollowerPatternTest {

    private LeaderFollowerPattern<LeaderFollowerPattern.Event> leaderFollower;
    private AtomicInteger processedEventCount;
    private Set<String> processingThreads;
    private AtomicBoolean processingError;

    @BeforeEach
    public void setUp() {
        processedEventCount = new AtomicInteger(0);
        processingThreads = ConcurrentHashMap.newKeySet();
        processingError = new AtomicBoolean(false);

        leaderFollower = new LeaderFollowerPattern<>(4, event -> {
            try {
                // Record the processing thread
                processingThreads.add(Thread.currentThread().getName());
                
                // Simulate processing
                Thread.sleep(10);
                processedEventCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processingError.set(true);
            } catch (Exception e) {
                processingError.set(true);
            }
        });
    }

    @AfterEach
    public void tearDown() {
        if (leaderFollower != null && leaderFollower.isRunning()) {
            leaderFollower.shutdown();
        }
    }

    @Test
    public void testBasicStartupAndShutdown() {
        assertFalse(leaderFollower.isRunning());
        assertEquals(0, leaderFollower.getActiveThreadCount());

        leaderFollower.start();
        assertTrue(leaderFollower.isRunning());
        
        // Wait for threads to initialize
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);
        assertEquals(4, leaderFollower.getActiveThreadCount());

        leaderFollower.shutdown();
        assertFalse(leaderFollower.isRunning());
        
        // Wait for threads to finish
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 0, 2000);
        assertEquals(0, leaderFollower.getActiveThreadCount());
    }

    @Test
    public void testEventProcessing() throws InterruptedException {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);

        // Submit some events
        for (int i = 0; i < 10; i++) {
            LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(i, "Test data " + i);
            assertTrue(leaderFollower.submitEvent(event));
        }

        // Wait for processing to complete
        waitForCondition(() -> leaderFollower.getProcessedEventCount() == 10, 5000);

        assertEquals(10, processedEventCount.get());
        assertEquals(10, leaderFollower.getProcessedEventCount());
        assertFalse(processingError.get());
    }

    @Test
    public void testLeaderPromotion() throws InterruptedException {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);

        int initialPromotions = leaderFollower.getLeaderPromotionCount();

        // Submit events to trigger leader promotions
        for (int i = 0; i < 5; i++) {
            LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(i, "Test data " + i);
            leaderFollower.submitEvent(event);
        }

        // Wait for processing
        waitForCondition(() -> leaderFollower.getProcessedEventCount() == 5, 3000);

        // Should have had leader promotions
        assertTrue(leaderFollower.getLeaderPromotionCount() > initialPromotions);
    }

    @Test
    public void testConcurrentEventSubmission() throws InterruptedException {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);

        final int threadCount = 5;
        final int eventsPerThread = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger submittedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(
                            threadId * 100 + j, 
                            "Data from thread " + threadId
                        );
                        if (leaderFollower.submitEvent(event)) {
                            submittedCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Wait for all events to be processed
        final int expectedEvents = threadCount * eventsPerThread;
        waitForCondition(() -> leaderFollower.getProcessedEventCount() == expectedEvents, 10000);

        assertEquals(expectedEvents, submittedCount.get());
        assertEquals(expectedEvents, leaderFollower.getProcessedEventCount());
        assertFalse(processingError.get());

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testMultipleThreadsProcessing() throws InterruptedException {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);

        // Submit enough events to ensure multiple threads get to process
        for (int i = 0; i < 20; i++) {
            LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(i, "Test data " + i);
            leaderFollower.submitEvent(event);
        }

        // Wait for processing
        waitForCondition(() -> leaderFollower.getProcessedEventCount() == 20, 10000);

        // Multiple threads should have participated in processing
        assertTrue(processingThreads.size() > 1, 
            "Expected multiple threads to process events, but only " + processingThreads.size() + " threads were used");
        assertFalse(processingError.get());
    }

    @Test
    public void testBlockingEventSubmission() throws InterruptedException {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);

        LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(1, "Blocking test");
        
        // This should not block since the queue is not full
        leaderFollower.submitEventBlocking(event);

        waitForCondition(() -> leaderFollower.getProcessedEventCount() == 1, 2000);
        assertEquals(1, leaderFollower.getProcessedEventCount());
    }

    @Test
    public void testSubmissionToShutdownPool() {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);
        
        leaderFollower.shutdown();
        waitForCondition(() -> !leaderFollower.isRunning(), 2000);

        LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(1, "Test after shutdown");
        assertFalse(leaderFollower.submitEvent(event));

        assertThrows(IllegalStateException.class, () -> {
            leaderFollower.submitEventBlocking(event);
        });
    }

    @Test
    public void testShutdownNow() throws InterruptedException {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);

        // Submit events
        for (int i = 0; i < 10; i++) {
            LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(i, "Test data " + i);
            leaderFollower.submitEvent(event);
        }

        // Immediate shutdown
        leaderFollower.shutdownNow();
        assertFalse(leaderFollower.isRunning());
        assertEquals(0, leaderFollower.getPendingEventCount()); // Queue should be cleared
    }

    @Test
    public void testStatistics() throws InterruptedException {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);

        assertEquals(4, leaderFollower.getActiveThreadCount());
        assertEquals(0, leaderFollower.getPendingEventCount());
        assertEquals(0, leaderFollower.getProcessedEventCount());
        assertTrue(leaderFollower.getLeaderPromotionCount() > 0); // Initial leader selection

        // Submit events
        for (int i = 0; i < 5; i++) {
            LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(i, "Test data " + i);
            leaderFollower.submitEvent(event);
        }

        // Wait for processing
        waitForCondition(() -> leaderFollower.getProcessedEventCount() == 5, 3000);

        assertEquals(5, leaderFollower.getProcessedEventCount());
        assertTrue(leaderFollower.getLeaderPromotionCount() > 1); // Should have promotions
    }

    @Test
    public void testCurrentLeader() throws InterruptedException {
        leaderFollower.start();
        waitForCondition(() -> leaderFollower.getActiveThreadCount() == 4, 2000);

        // Initially there should be a leader
        waitForCondition(() -> leaderFollower.getCurrentLeader() != null, 1000);
        assertNotNull(leaderFollower.getCurrentLeader());

        // Submit an event to trigger leader change
        LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(1, "Leader test");
        leaderFollower.submitEvent(event);

        // Wait for processing
        waitForCondition(() -> leaderFollower.getProcessedEventCount() == 1, 2000);

        // There should still be a leader (new one promoted)
        Thread.sleep(100); // Brief wait for leader promotion
        assertNotNull(leaderFollower.getCurrentLeader());
    }

    @Test
    public void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LeaderFollowerPattern<>(0, event -> {});
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new LeaderFollowerPattern<>(-1, event -> {});
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new LeaderFollowerPattern<>(4, null);
        });
    }

    @Test
    public void testEventClass() {
        LeaderFollowerPattern.Event event = new LeaderFollowerPattern.Event(42, "Test data");
        
        assertEquals(42, event.getId());
        assertEquals("Test data", event.getData());
        assertTrue(event.getTimestamp() > 0);
        assertNotNull(event.toString());
        assertTrue(event.toString().contains("42"));
        assertTrue(event.toString().contains("Test data"));
    }

    @Test
    public void testStressTest() throws InterruptedException {
        final int eventCount = 100;
        final AtomicInteger errorCount = new AtomicInteger(0);
        
        LeaderFollowerPattern<LeaderFollowerPattern.Event> stressTestPool = 
            new LeaderFollowerPattern<>(6, event -> {
                try {
                    // Simulate variable processing time
                    Thread.sleep(1 + (event.getId() % 5));
                    processedEventCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errorCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });

        try {
            stressTestPool.start();
            waitForCondition(() -> stressTestPool.getActiveThreadCount() == 6, 2000);

            // Submit events rapidly from multiple threads
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch submissionLatch = new CountDownLatch(4);

            for (int i = 0; i < 4; i++) {
                final int startId = i * 25;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 25; j++) {
                            LeaderFollowerPattern.Event event = 
                                new LeaderFollowerPattern.Event(startId + j, "Stress test data");
                            stressTestPool.submitEvent(event);
                        }
                    } finally {
                        submissionLatch.countDown();
                    }
                });
            }

            assertTrue(submissionLatch.await(5, TimeUnit.SECONDS));
            
            // Wait for processing to complete
            waitForCondition(() -> stressTestPool.getProcessedEventCount() == eventCount, 15000);

            assertEquals(eventCount, stressTestPool.getProcessedEventCount());
            assertEquals(0, errorCount.get());
            assertTrue(stressTestPool.getLeaderPromotionCount() > 6); // Should have many promotions

            executor.shutdown();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
            
        } finally {
            stressTestPool.shutdown();
        }
    }

    /**
     * Utility method to wait for a condition with timeout.
     */
    private void waitForCondition(java.util.concurrent.Callable<Boolean> condition, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                if (condition.call()) {
                    return;
                }
                Thread.sleep(10);
            } catch (Exception e) {
                // Ignore and continue waiting
            }
        }
    }
}