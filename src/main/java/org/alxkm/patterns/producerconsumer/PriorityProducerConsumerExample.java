package org.alxkm.patterns.producerconsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Producer-Consumer pattern using PriorityBlockingQueue.
 * This variation demonstrates priority-based processing where
 * high-priority items are consumed before lower-priority ones.
 */
public class PriorityProducerConsumerExample {

    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 2;
    private static final int ITEMS_PER_PRODUCER = 5;

    public static void main(String[] args) throws InterruptedException {
        PriorityBlockingQueue<PriorityItem> queue = new PriorityBlockingQueue<>();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS + NUM_CONSUMERS);

        try {
            // Start producers
            for (int i = 0; i < NUM_PRODUCERS; i++) {
                final int producerId = i;
                executor.submit(new PriorityProducer(queue, producerId, ITEMS_PER_PRODUCER));
            }

            // Start consumers
            for (int i = 0; i < NUM_CONSUMERS; i++) {
                final int consumerId = i;
                executor.submit(new PriorityConsumer(queue, consumerId));
            }

            // Let the system run for a while
            Thread.sleep(3000);

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Item with priority for priority-based processing
     */
    static class PriorityItem implements Comparable<PriorityItem> {
        private final String data;
        private final int priority;
        private final long timestamp;

        public PriorityItem(String data, int priority) {
            this.data = data;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public int compareTo(PriorityItem other) {
            // Higher priority first (reverse order)
            int result = Integer.compare(other.priority, this.priority);
            if (result == 0) {
                // If same priority, older items first
                result = Long.compare(this.timestamp, other.timestamp);
            }
            return result;
        }

        public String getData() {
            return data;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public String toString() {
            return data + " (priority: " + priority + ")";
        }
    }

    /**
     * Producer that creates items with different priorities
     */
    static class PriorityProducer implements Runnable {
        private final PriorityBlockingQueue<PriorityItem> queue;
        private final int producerId;
        private final int itemCount;

        public PriorityProducer(PriorityBlockingQueue<PriorityItem> queue, int producerId, int itemCount) {
            this.queue = queue;
            this.producerId = producerId;
            this.itemCount = itemCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < itemCount; i++) {
                    // Create items with random priorities (1-5, where 5 is highest)
                    int priority = (int) (Math.random() * 5) + 1;
                    PriorityItem item = new PriorityItem("Item-" + producerId + "-" + i, priority);
                    queue.put(item);
                    System.out.println("Producer " + producerId + " produced: " + item);
                    Thread.sleep(150); // Simulate production time
                }
                System.out.println("Producer " + producerId + " finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Producer " + producerId + " was interrupted");
            }
        }
    }

    /**
     * Consumer that processes items based on priority
     */
    static class PriorityConsumer implements Runnable {
        private final PriorityBlockingQueue<PriorityItem> queue;
        private final int consumerId;

        public PriorityConsumer(PriorityBlockingQueue<PriorityItem> queue, int consumerId) {
            this.queue = queue;
            this.consumerId = consumerId;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    PriorityItem item = queue.poll(1, TimeUnit.SECONDS);
                    if (item != null) {
                        System.out.println("Consumer " + consumerId + " consumed: " + item);
                        Thread.sleep(300); // Simulate processing time
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer " + consumerId + " was interrupted");
            }
        }
    }
}