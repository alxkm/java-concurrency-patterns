package org.alxkm.patterns.producerconsumer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Producer-Consumer pattern using DelayQueue for delayed processing.
 * This variation demonstrates processing items only after a specified delay,
 * useful for implementing task scheduling, caching with expiration, or
 * implementing retry mechanisms with backoff.
 */
public class DelayedProducerConsumerExample {

    private static final int NUM_PRODUCERS = 1;
    private static final int NUM_CONSUMERS = 2;
    private static final int ITEMS_TO_PRODUCE = 5;

    /** Delay used by the demo in main, in milliseconds. */
    private static final long DEMO_MAX_DELAY_MILLIS = 5_000;

    /**
     * What a run of the pattern moved through the queue.
     *
     * @param produced        how many tasks the producers scheduled.
     * @param consumed        how many tasks the consumers executed.
     * @param earlyDeliveries how many tasks came out before their delay had elapsed, which must be 0.
     */
    public record Result(int produced, int consumed, int earlyDeliveries) {
    }

    public static void main(String[] args) throws InterruptedException {
        Result result = run(NUM_PRODUCERS, NUM_CONSUMERS, ITEMS_TO_PRODUCE, DEMO_MAX_DELAY_MILLIS);
        System.out.printf("Scheduled: %d, Executed: %d, Delivered early: %d%n",
                result.produced(), result.consumed(), result.earlyDeliveries());
    }

    /**
     * Runs the pattern and returns once every scheduled task has been executed.
     *
     * The property that makes a DelayQueue a DelayQueue is that take never hands out an element whose
     * delay has not expired, no matter how many consumers are waiting. Each consumer therefore checks
     * its task against the deadline it was given and counts anything that arrived early.
     *
     * @param producers        how many producer tasks to run.
     * @param consumers        how many consumer tasks to run.
     * @param itemsPerProducer how many tasks each producer schedules.
     * @param maxDelayMillis   the largest delay a scheduled task may carry.
     * @return the scheduled, executed and early-delivery counts.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static Result run(int producers, int consumers, int itemsPerProducer, long maxDelayMillis)
            throws InterruptedException {
        DelayQueue<DelayedTask> queue = new DelayQueue<>();
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        AtomicInteger early = new AtomicInteger();
        CountDownLatch allConsumed = new CountDownLatch(producers * itemsPerProducer);
        ExecutorService executor = Executors.newFixedThreadPool(producers + consumers);

        try {
            for (int i = 0; i < producers; i++) {
                executor.submit(new DelayedProducer(queue, i, itemsPerProducer, maxDelayMillis, produced));
            }
            for (int i = 0; i < consumers; i++) {
                executor.submit(new DelayedConsumer(queue, i, consumed, early, allConsumed));
            }

            if (!allConsumed.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out with " + consumed.get() + " of "
                        + (producers * itemsPerProducer) + " tasks executed");
            }
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("executor did not terminate");
            }
        }
        return new Result(produced.get(), consumed.get(), early.get());
    }

    /**
     * Delayed task that implements Delayed interface
     */
    static class DelayedTask implements Delayed {
        private final String taskName;
        private final long executeTime;
        private final long delayMillis;

        public DelayedTask(String taskName, int delaySeconds) {
            this(taskName, TimeUnit.SECONDS.toMillis(delaySeconds));
        }

        public DelayedTask(String taskName, long delayMillis) {
            this.taskName = taskName;
            this.delayMillis = delayMillis;
            this.executeTime = System.currentTimeMillis() + delayMillis;
        }

        /**
         * The wall clock time this task becomes eligible, so a consumer can confirm it was not handed
         * one early.
         *
         * @return the deadline in milliseconds.
         */
        public long getExecuteTime() {
            return executeTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long remaining = executeTime - System.currentTimeMillis();
            return unit.convert(remaining, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(this.executeTime, ((DelayedTask) other).executeTime);
        }

        public String getTaskName() {
            return taskName;
        }

        public long getDelayMillis() {
            return delayMillis;
        }

        @Override
        public String toString() {
            return taskName + " (delayed " + delayMillis + "ms)";
        }
    }

    /**
     * Producer that creates delayed tasks with various delays
     */
    static class DelayedProducer implements Runnable {
        private final DelayQueue<DelayedTask> queue;
        private final int producerId;
        private final int itemCount;
        private final long maxDelayMillis;
        private final AtomicInteger producedCount;

        public DelayedProducer(DelayQueue<DelayedTask> queue, int producerId, int itemCount,
                               long maxDelayMillis, AtomicInteger producedCount) {
            this.queue = queue;
            this.producerId = producerId;
            this.itemCount = itemCount;
            this.maxDelayMillis = maxDelayMillis;
            this.producedCount = producedCount;
        }

        /**
         * Schedules every task. DelayQueue is unbounded, so put never blocks and nothing here throws.
         */
        @Override
        public void run() {
            for (int i = 0; i < itemCount; i++) {
                long delay = ThreadLocalRandom.current().nextLong(1, maxDelayMillis + 1);
                DelayedTask task = new DelayedTask("Task-" + producerId + "-" + i, delay);
                queue.put(task);
                producedCount.incrementAndGet();
                System.out.println("Producer " + producerId + " scheduled: " + task);
            }
            System.out.println("Producer " + producerId + " finished scheduling tasks");
        }
    }

    /**
     * Consumer that processes delayed tasks when they're ready
     */
    static class DelayedConsumer implements Runnable {
        private final DelayQueue<DelayedTask> queue;
        private final int consumerId;
        private final AtomicInteger consumedCount;
        private final AtomicInteger earlyCount;
        private final CountDownLatch allConsumed;

        public DelayedConsumer(DelayQueue<DelayedTask> queue, int consumerId,
                               AtomicInteger consumedCount, AtomicInteger earlyCount,
                               CountDownLatch allConsumed) {
            this.queue = queue;
            this.consumerId = consumerId;
            this.consumedCount = consumedCount;
            this.earlyCount = earlyCount;
            this.allConsumed = allConsumed;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    DelayedTask task = queue.poll(2, TimeUnit.SECONDS);
                    if (task != null) {
                        if (System.currentTimeMillis() < task.getExecuteTime()) {
                            earlyCount.incrementAndGet();
                        }
                        consumedCount.incrementAndGet();
                        allConsumed.countDown();
                        System.out.println("Consumer " + consumerId + " executed: " + task.getTaskName()
                                + " (was delayed " + task.getDelayMillis() + "ms)");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer " + consumerId + " was interrupted");
            }
        }
    }
}