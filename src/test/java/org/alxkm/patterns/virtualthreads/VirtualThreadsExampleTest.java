package org.alxkm.patterns.virtualthreads;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

public class VirtualThreadsExampleTest {

    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @AfterEach
    public void tearDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    public void testBasicVirtualThreadCreation() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Thread virtualThread = Thread.ofVirtual()
                .name("test-virtual-thread")
                .start(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        virtualThread.join();
        assertEquals(1, counter.get());
    }

    @Test
    public void testVirtualThreadWithExecutorService() throws InterruptedException {
        final int numTasks = 1000;
        AtomicInteger completedTasks = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numTasks);

        for (int i = 0; i < numTasks; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(10); // Simulate I/O work
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(numTasks, completedTasks.get());
    }

    @Test
    public void testVirtualThreadPerformanceVsPlatformThread() throws InterruptedException {
        final int numTasks = 1000;
        final int sleepTime = 50;

        // Test virtual threads
        Instant start = Instant.now();
        CountDownLatch virtualLatch = new CountDownLatch(numTasks);
        
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < numTasks; i++) {
                virtualExecutor.submit(() -> {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        virtualLatch.countDown();
                    }
                });
            }
            assertTrue(virtualLatch.await(10, TimeUnit.SECONDS));
        }
        Duration virtualTime = Duration.between(start, Instant.now());

        // Test platform threads
        start = Instant.now();
        CountDownLatch platformLatch = new CountDownLatch(numTasks);
        
        try (ExecutorService platformExecutor = Executors.newFixedThreadPool(100)) {
            for (int i = 0; i < numTasks; i++) {
                platformExecutor.submit(() -> {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        platformLatch.countDown();
                    }
                });
            }
            assertTrue(platformLatch.await(15, TimeUnit.SECONDS));
        }
        Duration platformTime = Duration.between(start, Instant.now());

        // Virtual threads should be at least as fast as platform threads for I/O-bound tasks
        assertTrue(virtualTime.toMillis() <= platformTime.toMillis() * 1.5, 
            String.format("Virtual: %dms, Platform: %dms", 
                virtualTime.toMillis(), platformTime.toMillis()));
    }

    @Test
    public void testProducerConsumerWithVirtualThreads() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(50);
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);
        final int totalItems = 100;

        CountDownLatch producerLatch = new CountDownLatch(1);
        CountDownLatch consumerLatch = new CountDownLatch(2);

        // Start producer
        Thread producer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < totalItems; i++) {
                    queue.put(i);
                    produced.incrementAndGet();
                }
                // Add poison pills for consumers
                queue.put(-1);
                queue.put(-1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                producerLatch.countDown();
            }
        });

        // Start consumers
        for (int i = 0; i < 2; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    while (true) {
                        Integer item = queue.take();
                        if (item == -1) {
                            queue.put(-1); // Re-add for other consumer
                            break;
                        }
                        consumed.incrementAndGet();
                        Thread.sleep(1); // Simulate processing
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    consumerLatch.countDown();
                }
            });
        }

        assertTrue(producerLatch.await(5, TimeUnit.SECONDS));
        assertTrue(consumerLatch.await(10, TimeUnit.SECONDS));
        producer.join();

        assertEquals(totalItems, produced.get());
        assertEquals(totalItems, consumed.get());
    }

    @Test
    public void testVirtualThreadWithCompletableFuture() throws Exception {
        AtomicInteger processedTasks = new AtomicInteger(0);

        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
                processedTasks.incrementAndGet();
                return "Task 1 completed";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Task 1 interrupted";
            }
        }, executor);

        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(150);
                processedTasks.incrementAndGet();
                return "Task 2 completed";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Task 2 interrupted";
            }
        }, executor);

        CompletableFuture<String> combined = future1.thenCombine(future2, (result1, result2) -> {
            processedTasks.incrementAndGet();
            return result1 + " | " + result2;
        });

        String result = combined.get(2, TimeUnit.SECONDS);
        
        assertNotNull(result);
        assertTrue(result.contains("Task 1 completed"));
        assertTrue(result.contains("Task 2 completed"));
        assertEquals(3, processedTasks.get()); // 2 tasks + 1 combination
    }

    @Test
    public void testMassiveVirtualThreadCreation() throws InterruptedException {
        final int numThreads = 10_000;
        AtomicInteger completedThreads = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numThreads);

        Instant start = Instant.now();

        for (int i = 0; i < numThreads; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(1); // Minimal work
                    completedThreads.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        Duration totalTime = Duration.between(start, Instant.now());

        assertEquals(numThreads, completedThreads.get());
        
        // Should complete reasonably quickly
        assertTrue(totalTime.toSeconds() < 10, 
            "Creating " + numThreads + " virtual threads took too long: " + totalTime.toSeconds() + "s");
    }

    @Test
    public void testVirtualThreadInterruption() throws InterruptedException {
        AtomicInteger interruptedCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(5);

        List<Thread> threads = new ArrayList<>();

        // Create threads that will be interrupted
        for (int i = 0; i < 5; i++) {
            Thread thread = Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(5000); // Long sleep
                } catch (InterruptedException e) {
                    interruptedCount.incrementAndGet();
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
            threads.add(thread);
        }

        // Let threads start waiting
        startLatch.countDown();
        Thread.sleep(100);

        // Interrupt all threads
        for (Thread thread : threads) {
            thread.interrupt();
        }

        assertTrue(endLatch.await(2, TimeUnit.SECONDS));
        assertEquals(5, interruptedCount.get());
    }

    @Test
    public void testVirtualThreadNameAndProperties() throws InterruptedException {
        AtomicInteger namedThreads = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        // Test custom thread names
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            Thread.ofVirtual()
                    .name("test-virtual-" + threadId)
                    .start(() -> {
                        Thread current = Thread.currentThread();
                        if (current.getName().startsWith("test-virtual-")) {
                            namedThreads.incrementAndGet();
                        }
                        assertTrue(current.isVirtual());
                        latch.countDown();
                    });
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(3, namedThreads.get());
    }

    @Test
    public void testVirtualThreadExceptionHandling() throws InterruptedException {
        AtomicInteger exceptionsHandled = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            if (thread.isVirtual()) {
                exceptionsHandled.incrementAndGet();
            }
            latch.countDown();
        });

        try {
            // Create threads that will throw exceptions
            for (int i = 0; i < 3; i++) {
                final int threadId = i;
                Thread.ofVirtual().start(() -> {
                    throw new RuntimeException("Test exception " + threadId);
                });
            }

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(3, exceptionsHandled.get());

        } finally {
            Thread.setDefaultUncaughtExceptionHandler(null);
        }
    }

    @Test
    public void testVirtualThreadPoolShutdown() throws InterruptedException {
        ExecutorService testExecutor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger completedTasks = new AtomicInteger(0);

        // Submit some tasks
        for (int i = 0; i < 10; i++) {
            testExecutor.submit(() -> {
                try {
                    Thread.sleep(100);
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Shutdown and wait
        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(testExecutor.isShutdown());
        assertTrue(testExecutor.isTerminated());

        // All tasks should have completed
        assertEquals(10, completedTasks.get());
    }

    @Test
    public void testVirtualThreadFactoryCustomization() throws InterruptedException {
        AtomicInteger factoryCreatedThreads = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);

        ThreadFactory virtualFactory = Thread.ofVirtual()
                .name("factory-virtual-", 1)
                .factory();

        try (ExecutorService customExecutor = Executors.newThreadPerTaskExecutor(virtualFactory)) {
            for (int i = 0; i < 5; i++) {
                customExecutor.submit(() -> {
                    Thread current = Thread.currentThread();
                    if (current.getName().startsWith("factory-virtual-")) {
                        factoryCreatedThreads.incrementAndGet();
                    }
                    assertTrue(current.isVirtual());
                    latch.countDown();
                });
            }

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(5, factoryCreatedThreads.get());
        }
    }
}