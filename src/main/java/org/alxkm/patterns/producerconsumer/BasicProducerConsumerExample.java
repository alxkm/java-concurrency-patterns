package org.alxkm.patterns.producerconsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Basic Producer-Consumer pattern implementation using ArrayBlockingQueue.
 * This example demonstrates the fundamental producer-consumer pattern where
 * producers generate data and consumers process it asynchronously.
 */
public class BasicProducerConsumerExample {

    private static final int QUEUE_CAPACITY = 10;
    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 3;
    private static final int ITEMS_PER_PRODUCER = 5;

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS + NUM_CONSUMERS);

        try {
            // Start producers
            for (int i = 0; i < NUM_PRODUCERS; i++) {
                final int producerId = i;
                executor.submit(new Producer(queue, producerId, ITEMS_PER_PRODUCER));
            }

            // Start consumers
            for (int i = 0; i < NUM_CONSUMERS; i++) {
                final int consumerId = i;
                executor.submit(new Consumer(queue, consumerId));
            }

            // Let the system run for a while
            Thread.sleep(5000);

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Producer class that generates items and puts them into the queue
     */
    static class Producer implements Runnable {
        private final BlockingQueue<String> queue;
        private final int producerId;
        private final int itemCount;

        public Producer(BlockingQueue<String> queue, int producerId, int itemCount) {
            this.queue = queue;
            this.producerId = producerId;
            this.itemCount = itemCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < itemCount; i++) {
                    String item = "Item-" + producerId + "-" + i;
                    queue.put(item); // Blocks if queue is full
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
     * Consumer class that takes items from the queue and processes them
     */
    static class Consumer implements Runnable {
        private final BlockingQueue<String> queue;
        private final int consumerId;

        public Consumer(BlockingQueue<String> queue, int consumerId) {
            this.queue = queue;
            this.consumerId = consumerId;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String item = queue.poll(1, TimeUnit.SECONDS); // Wait up to 1 second for item
                    if (item != null) {
                        System.out.println("Consumer " + consumerId + " consumed: " + item);
                        Thread.sleep(200); // Simulate processing time
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer " + consumerId + " was interrupted");
            }
        }
    }
}