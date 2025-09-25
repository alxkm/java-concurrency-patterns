package org.alxkm.patterns.producerconsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Producer-Consumer pattern with batch processing.
 * This variation demonstrates processing items in batches for improved efficiency,
 * useful for database operations, I/O operations, or any scenario where
 * batch processing provides better performance than individual item processing.
 */
public class BatchProducerConsumerExample {

    private static final int QUEUE_CAPACITY = 20;
    private static final int NUM_PRODUCERS = 3;
    private static final int NUM_CONSUMERS = 2;
    private static final int ITEMS_PER_PRODUCER = 10;
    private static final int BATCH_SIZE = 5;

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS + NUM_CONSUMERS);

        try {
            // Start producers
            for (int i = 0; i < NUM_PRODUCERS; i++) {
                final int producerId = i;
                executor.submit(new BatchProducer(queue, producerId, ITEMS_PER_PRODUCER));
            }

            // Start batch consumers
            for (int i = 0; i < NUM_CONSUMERS; i++) {
                final int consumerId = i;
                executor.submit(new BatchConsumer(queue, consumerId, BATCH_SIZE));
            }

            // Let the system run for a while
            Thread.sleep(8000);

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Producer that generates individual items
     */
    static class BatchProducer implements Runnable {
        private final BlockingQueue<String> queue;
        private final int producerId;
        private final int itemCount;

        public BatchProducer(BlockingQueue<String> queue, int producerId, int itemCount) {
            this.queue = queue;
            this.producerId = producerId;
            this.itemCount = itemCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < itemCount; i++) {
                    String item = "Item-" + producerId + "-" + i;
                    queue.put(item);
                    System.out.println("Producer " + producerId + " produced: " + item);
                    Thread.sleep(100); // Simulate production time
                }
                System.out.println("Producer " + producerId + " finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Producer " + producerId + " was interrupted");
            }
        }
    }

    /**
     * Consumer that processes items in batches
     */
    static class BatchConsumer implements Runnable {
        private final BlockingQueue<String> queue;
        private final int consumerId;
        private final int batchSize;

        public BatchConsumer(BlockingQueue<String> queue, int consumerId, int batchSize) {
            this.queue = queue;
            this.consumerId = consumerId;
            this.batchSize = batchSize;
        }

        @Override
        public void run() {
            try {
                List<String> batch = new ArrayList<>();
                
                while (!Thread.currentThread().isInterrupted()) {
                    // Collect items for batch processing
                    String item = queue.poll(1, TimeUnit.SECONDS);
                    if (item != null) {
                        batch.add(item);
                        System.out.println("Consumer " + consumerId + " collected: " + item + 
                                         " (batch size: " + batch.size() + "/" + batchSize + ")");
                        
                        // Process batch when it reaches the desired size
                        if (batch.size() >= batchSize) {
                            processBatch(batch);
                            batch.clear();
                        }
                    } else if (!batch.isEmpty()) {
                        // Process remaining items if timeout occurred and we have items
                        System.out.println("Consumer " + consumerId + " processing partial batch due to timeout");
                        processBatch(batch);
                        batch.clear();
                    }
                }
                
                // Process any remaining items before shutting down
                if (!batch.isEmpty()) {
                    System.out.println("Consumer " + consumerId + " processing final batch");
                    processBatch(batch);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer " + consumerId + " was interrupted");
            }
        }

        private void processBatch(List<String> batch) throws InterruptedException {
            System.out.println("Consumer " + consumerId + " processing batch of " + batch.size() + 
                             " items: " + batch);
            
            // Simulate batch processing (e.g., database batch insert, file I/O, etc.)
            Thread.sleep(500);
            
            System.out.println("Consumer " + consumerId + " completed batch processing of " + 
                             batch.size() + " items");
        }
    }
}