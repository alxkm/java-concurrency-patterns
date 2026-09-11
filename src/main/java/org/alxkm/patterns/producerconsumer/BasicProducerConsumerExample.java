package org.alxkm.patterns.producerconsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * What a run of the pattern produced and consumed.
     *
     * @param produced how many items the producers put on the queue.
     * @param consumed how many items the consumers took off it.
     */
    public record Result(int produced, int consumed) {
    }

    public static void main(String[] args) throws InterruptedException {
        Result result = run(NUM_PRODUCERS, NUM_CONSUMERS, ITEMS_PER_PRODUCER);
        System.out.printf("Produced: %d, Consumed: %d%n", result.produced(), result.consumed());
    }

    /**
     * Runs the pattern and returns once every produced item has been consumed.
     *
     * Waiting on a latch rather than sleeping a fixed interval is what makes the outcome meaningful:
     * a sleep either cuts the run short on a slow machine or wastes time on a fast one, and either way
     * the counts it reports depend on the clock rather than on the code.
     *
     * @param producers        how many producer tasks to run.
     * @param consumers        how many consumer tasks to run.
     * @param itemsPerProducer how many items each producer publishes.
     * @return the produced and consumed counts, which should be equal.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static Result run(int producers, int consumers, int itemsPerProducer) throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        CountDownLatch allConsumed = new CountDownLatch(producers * itemsPerProducer);
        ExecutorService executor = Executors.newFixedThreadPool(producers + consumers);

        try {
            for (int i = 0; i < producers; i++) {
                executor.submit(new Producer(queue, i, itemsPerProducer, produced));
            }
            for (int i = 0; i < consumers; i++) {
                executor.submit(new Consumer(queue, i, consumed, allConsumed));
            }

            if (!allConsumed.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "timed out with " + consumed.get() + " of " + (producers * itemsPerProducer)
                                + " items consumed");
            }
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("executor did not terminate");
            }
        }
        return new Result(produced.get(), consumed.get());
    }

    /**
     * Producer class that generates items and puts them into the queue
     */
    static class Producer implements Runnable {
        private final BlockingQueue<String> queue;
        private final int producerId;
        private final int itemCount;
        private final AtomicInteger producedCount;

        public Producer(BlockingQueue<String> queue, int producerId, int itemCount,
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
                    queue.put(item); // Blocks if queue is full
                    producedCount.incrementAndGet();
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
        private final AtomicInteger consumedCount;
        private final CountDownLatch allConsumed;

        public Consumer(BlockingQueue<String> queue, int consumerId, AtomicInteger consumedCount,
                        CountDownLatch allConsumed) {
            this.queue = queue;
            this.consumerId = consumerId;
            this.consumedCount = consumedCount;
            this.allConsumed = allConsumed;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String item = queue.poll(1, TimeUnit.SECONDS); // Wait up to 1 second for item
                    if (item != null) {
                        consumedCount.incrementAndGet();
                        allConsumed.countDown();
                        System.out.println("Consumer " + consumerId + " consumed: " + item);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer " + consumerId + " was interrupted");
            }
        }
    }
}