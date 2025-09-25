package org.alxkm.patterns.producerconsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProducerConsumerPatternsTest {

    /**
     * Test basic producer-consumer pattern with ArrayBlockingQueue
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testBasicProducerConsumer() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);
        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);
        CountDownLatch producersFinished = new CountDownLatch(2);
        CountDownLatch consumerStarted = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            // Start producer threads
            for (int i = 0; i < 2; i++) {
                final int producerId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 5; j++) {
                            String item = "Item-" + producerId + "-" + j;
                            queue.put(item);
                            producedCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        producersFinished.countDown();
                    }
                });
            }

            // Start consumer thread
            executor.submit(() -> {
                try {
                    consumerStarted.countDown();
                    while (!Thread.currentThread().isInterrupted()) {
                        String item = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (item != null) {
                            consumedCount.incrementAndGet();
                        }
                        
                        // Stop if all producers are done and queue is empty
                        if (producersFinished.getCount() == 0 && queue.isEmpty()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Wait for consumer to start
            consumerStarted.await();
            
            // Wait for producers to finish
            producersFinished.await();
            
            // Give consumer time to process remaining items
            Thread.sleep(200);

            assertEquals(10, producedCount.get(), "Should have produced 10 items");
            assertEquals(10, consumedCount.get(), "Should have consumed all produced items");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Test priority-based producer-consumer pattern
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testPriorityProducerConsumer() throws InterruptedException {
        PriorityBlockingQueue<PriorityProducerConsumerExample.PriorityItem> queue = new PriorityBlockingQueue<>();
        AtomicBoolean highPriorityProcessedFirst = new AtomicBoolean(false);
        CountDownLatch itemsProduced = new CountDownLatch(2);
        CountDownLatch firstItemConsumed = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Producer thread - produce low priority then high priority
            executor.submit(() -> {
                try {
                    // Add low priority item first
                    PriorityProducerConsumerExample.PriorityItem lowPriority = 
                        new PriorityProducerConsumerExample.PriorityItem("Low Priority", 1);
                    queue.put(lowPriority);
                    itemsProduced.countDown();

                    Thread.sleep(50); // Small delay

                    // Add high priority item
                    PriorityProducerConsumerExample.PriorityItem highPriority = 
                        new PriorityProducerConsumerExample.PriorityItem("High Priority", 5);
                    queue.put(highPriority);
                    itemsProduced.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Consumer thread
            executor.submit(() -> {
                try {
                    // Wait for both items to be produced
                    itemsProduced.await();
                    
                    // First consumed item should be high priority
                    PriorityProducerConsumerExample.PriorityItem firstItem = queue.take();
                    if (firstItem.getPriority() == 5) {
                        highPriorityProcessedFirst.set(true);
                    }
                    firstItemConsumed.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            firstItemConsumed.await();
            assertTrue(highPriorityProcessedFirst.get(), 
                      "High priority item should be processed before low priority item");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Test delayed producer-consumer pattern
     */
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void testDelayedProducerConsumer() throws InterruptedException {
        DelayQueue<DelayedProducerConsumerExample.DelayedTask> queue = new DelayQueue<>();
        AtomicBoolean delayRespected = new AtomicBoolean(true);
        CountDownLatch taskConsumed = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            long startTime = System.currentTimeMillis();
            int delaySeconds = 2;

            // Producer thread
            executor.submit(() -> {
                DelayedProducerConsumerExample.DelayedTask task = 
                    new DelayedProducerConsumerExample.DelayedTask("Test Task", delaySeconds);
                queue.put(task);
            });

            // Consumer thread
            executor.submit(() -> {
                try {
                    DelayedProducerConsumerExample.DelayedTask task = queue.take();
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    
                    // Check if delay was respected (allow some tolerance for timing)
                    if (elapsedTime < (delaySeconds * 1000 - 100)) {
                        delayRespected.set(false);
                    }
                    
                    taskConsumed.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            taskConsumed.await();
            assertTrue(delayRespected.get(), "Task should not be consumed before delay expires");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Test transfer queue synchronous handoff
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testTransferQueue() throws InterruptedException {
        LinkedTransferQueue<String> queue = new LinkedTransferQueue<>();
        AtomicBoolean transferCompleted = new AtomicBoolean(false);
        AtomicBoolean consumerReady = new AtomicBoolean(false);
        CountDownLatch consumerStarted = new CountDownLatch(1);
        CountDownLatch transferDone = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Consumer thread
            executor.submit(() -> {
                try {
                    consumerStarted.countDown();
                    consumerReady.set(true);
                    String item = queue.take();
                    assertEquals("Test Item", item);
                    transferCompleted.set(true);
                    transferDone.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Wait for consumer to start
            consumerStarted.await();
            Thread.sleep(100); // Give consumer time to be ready

            // Producer thread
            executor.submit(() -> {
                try {
                    // Should transfer directly to waiting consumer
                    assertTrue(queue.hasWaitingConsumer(), "Should have waiting consumer");
                    queue.transfer("Test Item");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            transferDone.await();
            assertTrue(transferCompleted.get(), "Transfer should complete successfully");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Test batch processing pattern
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testBatchProducerConsumer() throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        AtomicInteger batchCount = new AtomicInteger(0);
        AtomicInteger totalProcessed = new AtomicInteger(0);
        CountDownLatch productionComplete = new CountDownLatch(1);
        CountDownLatch processingComplete = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        final int BATCH_SIZE = 3;
        final int TOTAL_ITEMS = 7; // Will create 2 full batches + 1 partial

        try {
            // Producer thread
            executor.submit(() -> {
                try {
                    for (int i = 0; i < TOTAL_ITEMS; i++) {
                        queue.put("Item-" + i);
                    }
                    productionComplete.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Batch consumer thread
            executor.submit(() -> {
                try {
                    productionComplete.await();
                    Thread.sleep(100); // Let all items be produced
                    
                    java.util.List<String> batch = new java.util.ArrayList<>();
                    
                    while (totalProcessed.get() < TOTAL_ITEMS) {
                        String item = queue.poll(500, TimeUnit.MILLISECONDS);
                        if (item != null) {
                            batch.add(item);
                            
                            if (batch.size() >= BATCH_SIZE || queue.isEmpty()) {
                                // Process batch
                                totalProcessed.addAndGet(batch.size());
                                batchCount.incrementAndGet();
                                batch.clear();
                            }
                        }
                    }
                    
                    processingComplete.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            processingComplete.await();

            assertEquals(TOTAL_ITEMS, totalProcessed.get(), "Should process all items");
            assertEquals(3, batchCount.get(), "Should create 3 batches (2 full + 1 partial)");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}