package org.alxkm.patterns.producerconsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producer-Consumer pattern using PriorityBlockingQueue.
 * This variation demonstrates priority-based processing where
 * high-priority items are consumed before lower-priority ones.
 */
public class PriorityProducerConsumerExample {

    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 2;
    private static final int ITEMS_PER_PRODUCER = 5;

    /**
     * What a run of the pattern moved through the queue.
     *
     * @param produced how many items the producers published.
     * @param consumed how many items the consumers took.
     */
    public record Result(int produced, int consumed) {
    }

    public static void main(String[] args) throws InterruptedException {
        Result result = run(NUM_PRODUCERS, NUM_CONSUMERS, ITEMS_PER_PRODUCER);
        System.out.printf("Produced: %d, Consumed: %d%n", result.produced(), result.consumed());
        System.out.println("Draining a preloaded queue in order: " + drainInPriorityOrder(10));
    }

    /**
     * Runs the pattern and returns once every produced item has been consumed.
     *
     * Note what this does not promise. With producers still publishing while consumers take, a
     * consumer can only pick the highest priority item present at that moment, so the sequence
     * observed here is not sorted. Priority ordering is a property of each take rather than of the
     * run, and {@link #drainInPriorityOrder(int)} is where it can actually be asserted.
     *
     * @param producers        how many producer tasks to run.
     * @param consumers        how many consumer tasks to run.
     * @param itemsPerProducer how many items each producer publishes.
     * @return the produced and consumed counts, which should be equal.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static Result run(int producers, int consumers, int itemsPerProducer)
            throws InterruptedException {
        PriorityBlockingQueue<PriorityItem> queue = new PriorityBlockingQueue<>();
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        CountDownLatch allConsumed = new CountDownLatch(producers * itemsPerProducer);
        ExecutorService executor = Executors.newFixedThreadPool(producers + consumers);

        try {
            for (int i = 0; i < producers; i++) {
                executor.submit(new PriorityProducer(queue, i, itemsPerProducer, produced));
            }
            for (int i = 0; i < consumers; i++) {
                executor.submit(new PriorityConsumer(queue, i, consumed, allConsumed));
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
        return new Result(produced.get(), consumed.get());
    }

    /**
     * Fills the queue, then drains it, returning the priorities in the order they came out.
     *
     * Everything is published before anything is taken, which is what makes the result deterministic:
     * the queue holds every item at once, so each take must return the highest priority remaining and
     * the sequence comes out non-increasing.
     *
     * @param itemCount how many items to publish and then drain.
     * @return the priorities in consumption order.
     */
    public static List<Integer> drainInPriorityOrder(int itemCount) {
        PriorityBlockingQueue<PriorityItem> queue = new PriorityBlockingQueue<>();
        for (int i = 0; i < itemCount; i++) {
            queue.put(new PriorityItem("Item-" + i, (i % 5) + 1));
        }

        List<Integer> order = new ArrayList<>(itemCount);
        PriorityItem item;
        while ((item = queue.poll()) != null) {
            order.add(item.getPriority());
        }
        return order;
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
        private final AtomicInteger producedCount;

        public PriorityProducer(PriorityBlockingQueue<PriorityItem> queue, int producerId, int itemCount,
                                AtomicInteger producedCount) {
            this.queue = queue;
            this.producerId = producerId;
            this.itemCount = itemCount;
            this.producedCount = producedCount;
        }

        /**
         * Publishes every item. There is no interruption handling because there is nothing here that
         * blocks: PriorityBlockingQueue is unbounded, so put never waits and never throws.
         */
        @Override
        public void run() {
            for (int i = 0; i < itemCount; i++) {
                // Create items with random priorities (1-5, where 5 is highest)
                int priority = (int) (Math.random() * 5) + 1;
                PriorityItem item = new PriorityItem("Item-" + producerId + "-" + i, priority);
                queue.put(item);
                producedCount.incrementAndGet();
                System.out.println("Producer " + producerId + " produced: " + item);
            }
            System.out.println("Producer " + producerId + " finished");
        }
    }

    /**
     * Consumer that processes items based on priority
     */
    static class PriorityConsumer implements Runnable {
        private final PriorityBlockingQueue<PriorityItem> queue;
        private final int consumerId;
        private final AtomicInteger consumedCount;
        private final CountDownLatch allConsumed;

        public PriorityConsumer(PriorityBlockingQueue<PriorityItem> queue, int consumerId,
                                AtomicInteger consumedCount, CountDownLatch allConsumed) {
            this.queue = queue;
            this.consumerId = consumerId;
            this.consumedCount = consumedCount;
            this.allConsumed = allConsumed;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    PriorityItem item = queue.poll(1, TimeUnit.SECONDS);
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