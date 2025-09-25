package org.alxkm.patterns.producerconsumer;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    public static void main(String[] args) throws InterruptedException {
        DelayQueue<DelayedTask> queue = new DelayQueue<>();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS + NUM_CONSUMERS);

        try {
            // Start producers
            for (int i = 0; i < NUM_PRODUCERS; i++) {
                final int producerId = i;
                executor.submit(new DelayedProducer(queue, producerId, ITEMS_TO_PRODUCE));
            }

            // Start consumers
            for (int i = 0; i < NUM_CONSUMERS; i++) {
                final int consumerId = i;
                executor.submit(new DelayedConsumer(queue, consumerId));
            }

            // Let the system run for a while
            Thread.sleep(10000);

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Delayed task that implements Delayed interface
     */
    static class DelayedTask implements Delayed {
        private final String taskName;
        private final long executeTime;
        private final int delaySeconds;

        public DelayedTask(String taskName, int delaySeconds) {
            this.taskName = taskName;
            this.delaySeconds = delaySeconds;
            this.executeTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds);
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

        public int getDelaySeconds() {
            return delaySeconds;
        }

        @Override
        public String toString() {
            return taskName + " (delayed " + delaySeconds + "s)";
        }
    }

    /**
     * Producer that creates delayed tasks with various delays
     */
    static class DelayedProducer implements Runnable {
        private final DelayQueue<DelayedTask> queue;
        private final int producerId;
        private final int itemCount;

        public DelayedProducer(DelayQueue<DelayedTask> queue, int producerId, int itemCount) {
            this.queue = queue;
            this.producerId = producerId;
            this.itemCount = itemCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < itemCount; i++) {
                    // Create tasks with random delays (1-5 seconds)
                    int delaySeconds = (int) (Math.random() * 5) + 1;
                    DelayedTask task = new DelayedTask("Task-" + producerId + "-" + i, delaySeconds);
                    queue.put(task);
                    System.out.println("Producer " + producerId + " scheduled: " + task + 
                                     " at " + System.currentTimeMillis());
                    Thread.sleep(500); // Simulate production time
                }
                System.out.println("Producer " + producerId + " finished scheduling tasks");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Producer " + producerId + " was interrupted");
            }
        }
    }

    /**
     * Consumer that processes delayed tasks when they're ready
     */
    static class DelayedConsumer implements Runnable {
        private final DelayQueue<DelayedTask> queue;
        private final int consumerId;

        public DelayedConsumer(DelayQueue<DelayedTask> queue, int consumerId) {
            this.queue = queue;
            this.consumerId = consumerId;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    DelayedTask task = queue.poll(2, TimeUnit.SECONDS);
                    if (task != null) {
                        System.out.println("Consumer " + consumerId + " executed: " + task.getTaskName() + 
                                         " at " + System.currentTimeMillis() + 
                                         " (was delayed " + task.getDelaySeconds() + "s)");
                        Thread.sleep(100); // Simulate processing time
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer " + consumerId + " was interrupted");
            }
        }
    }
}