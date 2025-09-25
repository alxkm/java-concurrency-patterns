package org.alxkm.patterns.producerconsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/**
 * Producer-Consumer pattern using LinkedTransferQueue for synchronous handoffs.
 * This variation demonstrates direct handoff between producers and consumers,
 * where producers can wait for consumers to be available before transferring items.
 */
public class TransferQueueExample {

    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 1;
    private static final int ITEMS_PER_PRODUCER = 3;

    public static void main(String[] args) throws InterruptedException {
        LinkedTransferQueue<String> queue = new LinkedTransferQueue<>();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS + NUM_CONSUMERS);

        try {
            // Start consumers first
            for (int i = 0; i < NUM_CONSUMERS; i++) {
                final int consumerId = i;
                executor.submit(new TransferConsumer(queue, consumerId));
            }

            // Give consumers time to start
            Thread.sleep(500);

            // Start producers
            for (int i = 0; i < NUM_PRODUCERS; i++) {
                final int producerId = i;
                executor.submit(new TransferProducer(queue, producerId, ITEMS_PER_PRODUCER));
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
     * Producer that uses transfer() for synchronous handoff
     */
    static class TransferProducer implements Runnable {
        private final LinkedTransferQueue<String> queue;
        private final int producerId;
        private final int itemCount;

        public TransferProducer(LinkedTransferQueue<String> queue, int producerId, int itemCount) {
            this.queue = queue;
            this.producerId = producerId;
            this.itemCount = itemCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < itemCount; i++) {
                    String item = "Item-" + producerId + "-" + i;
                    
                    // Check if there are waiting consumers
                    if (queue.hasWaitingConsumer()) {
                        System.out.println("Producer " + producerId + " found waiting consumer, transferring: " + item);
                        queue.transfer(item); // Synchronous handoff - blocks until consumed
                        System.out.println("Producer " + producerId + " transfer completed for: " + item);
                    } else {
                        System.out.println("Producer " + producerId + " no waiting consumer, trying transfer with timeout: " + item);
                        boolean transferred = queue.tryTransfer(item, 2, TimeUnit.SECONDS);
                        if (transferred) {
                            System.out.println("Producer " + producerId + " transfer succeeded for: " + item);
                        } else {
                            System.out.println("Producer " + producerId + " transfer timed out, putting to queue: " + item);
                            queue.put(item); // Fall back to normal queue behavior
                        }
                    }
                    
                    Thread.sleep(1000); // Simulate production time
                }
                System.out.println("Producer " + producerId + " finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Producer " + producerId + " was interrupted");
            }
        }
    }

    /**
     * Consumer that takes items from the transfer queue
     */
    static class TransferConsumer implements Runnable {
        private final LinkedTransferQueue<String> queue;
        private final int consumerId;

        public TransferConsumer(LinkedTransferQueue<String> queue, int consumerId) {
            this.queue = queue;
            this.consumerId = consumerId;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("Consumer " + consumerId + " waiting for item...");
                    String item = queue.poll(3, TimeUnit.SECONDS);
                    if (item != null) {
                        System.out.println("Consumer " + consumerId + " received: " + item);
                        Thread.sleep(2000); // Simulate longer processing time
                        System.out.println("Consumer " + consumerId + " finished processing: " + item);
                    } else {
                        System.out.println("Consumer " + consumerId + " timed out waiting for item");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer " + consumerId + " was interrupted");
            }
        }
    }
}