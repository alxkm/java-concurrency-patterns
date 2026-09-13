package org.alxkm.patterns.producerconsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * What a run of the pattern moved through the queue.
     *
     * @param produced how many items the producers published.
     * @param consumed how many items the consumers took.
     * @param batches  how many batches the consumers processed.
     */
    public record Result(int produced, int consumed, int batches) {
    }

    public static void main(String[] args) throws InterruptedException {
        Result result = run(NUM_PRODUCERS, NUM_CONSUMERS, ITEMS_PER_PRODUCER, BATCH_SIZE);
        System.out.printf("Produced: %d, Consumed: %d, Batches: %d%n",
                result.produced(), result.consumed(), result.batches());
    }

    /**
     * Runs the pattern and returns once every produced item has been consumed.
     *
     * @param producers        how many producer tasks to run.
     * @param consumers        how many consumer tasks to run.
     * @param itemsPerProducer how many items each producer publishes.
     * @param batchSize        how many items a consumer collects before processing them together.
     * @return the produced, consumed and batch counts.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static Result run(int producers, int consumers, int itemsPerProducer, int batchSize)
            throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        AtomicInteger batches = new AtomicInteger();
        CountDownLatch allConsumed = new CountDownLatch(producers * itemsPerProducer);
        ExecutorService executor = Executors.newFixedThreadPool(producers + consumers);

        try {
            for (int i = 0; i < producers; i++) {
                executor.submit(new BatchProducer(queue, i, itemsPerProducer, produced));
            }
            for (int i = 0; i < consumers; i++) {
                executor.submit(new BatchConsumer(queue, i, batchSize, consumed, batches, allConsumed));
            }

            if (!allConsumed.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out with " + consumed.get() + " of "
                        + (producers * itemsPerProducer) + " items consumed");
            }
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("executor did not terminate");
            }
        }
        return new Result(produced.get(), consumed.get(), batches.get());
    }

    /**
     * Producer that generates individual items
     */
    static class BatchProducer implements Runnable {
        private final BlockingQueue<String> queue;
        private final int producerId;
        private final int itemCount;
        private final AtomicInteger producedCount;

        public BatchProducer(BlockingQueue<String> queue, int producerId, int itemCount,
                             AtomicInteger producedCount) {
            this.queue = queue;
            this.producerId = producerId;
            this.itemCount = itemCount;
            this.producedCount = producedCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < itemCount; i++) {
                    String item = "Item-" + producerId + "-" + i;
                    queue.put(item);
                    producedCount.incrementAndGet();
                    System.out.println("Producer " + producerId + " produced: " + item);
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
        private final AtomicInteger consumedCount;
        private final AtomicInteger batchCount;
        private final CountDownLatch allConsumed;

        public BatchConsumer(BlockingQueue<String> queue, int consumerId, int batchSize,
                             AtomicInteger consumedCount, AtomicInteger batchCount,
                             CountDownLatch allConsumed) {
            this.queue = queue;
            this.consumerId = consumerId;
            this.batchSize = batchSize;
            this.consumedCount = consumedCount;
            this.batchCount = batchCount;
            this.allConsumed = allConsumed;
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

        private void processBatch(List<String> batch) {
            System.out.println("Consumer " + consumerId + " processing batch of " + batch.size()
                    + " items: " + batch);

            batchCount.incrementAndGet();
            for (int i = 0; i < batch.size(); i++) {
                consumedCount.incrementAndGet();
                allConsumed.countDown();
            }

            System.out.println("Consumer " + consumerId + " completed batch processing of "
                    + batch.size() + " items");
        }
    }
}