package org.alxkm.patterns.producerconsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producer-Consumer pattern using LinkedTransferQueue for synchronous handoffs.
 * This variation demonstrates direct handoff between producers and consumers,
 * where producers can wait for consumers to be available before transferring items.
 */
public class TransferQueueExample {

    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 1;
    private static final int ITEMS_PER_PRODUCER = 3;

    /**
     * What a run of the pattern moved through the queue.
     *
     * @param produced how many items the producers published.
     * @param consumed how many items the consumers received.
     */
    public record Result(int produced, int consumed) {
    }

    public static void main(String[] args) throws InterruptedException {
        Result result = run(NUM_PRODUCERS, NUM_CONSUMERS, ITEMS_PER_PRODUCER);
        System.out.printf("Produced: %d, Consumed: %d%n", result.produced(), result.consumed());
        System.out.println("transfer() blocked until a consumer arrived: " + transferWaitsForAConsumer(200));
    }

    /**
     * Runs the pattern and returns once every produced item has been received.
     *
     * @param producers        how many producer tasks to run.
     * @param consumers        how many consumer tasks to run.
     * @param itemsPerProducer how many items each producer publishes.
     * @return the produced and consumed counts, which should be equal.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static Result run(int producers, int consumers, int itemsPerProducer)
            throws InterruptedException {
        LinkedTransferQueue<String> queue = new LinkedTransferQueue<>();
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        CountDownLatch allConsumed = new CountDownLatch(producers * itemsPerProducer);
        ExecutorService executor = Executors.newFixedThreadPool(producers + consumers);

        try {
            for (int i = 0; i < consumers; i++) {
                executor.submit(new TransferConsumer(queue, i, consumed, allConsumed));
            }
            for (int i = 0; i < producers; i++) {
                executor.submit(new TransferProducer(queue, i, itemsPerProducer, produced));
            }

            if (!allConsumed.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out with " + consumed.get() + " of "
                        + (producers * itemsPerProducer) + " items received");
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
     * Demonstrates the one thing a TransferQueue adds over an ordinary queue.
     *
     * {@code put} hands the element to the queue and returns. {@code transfer} hands it to a consumer
     * and does not return until one has taken it, so the producer learns that the work was picked up
     * rather than merely queued. That is the difference between buffering and a rendezvous.
     *
     * @param observeMillis how long to watch the producer before releasing it, in milliseconds.
     * @return true if the producer was still blocked in transfer() when nobody had taken the item yet.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static boolean transferWaitsForAConsumer(long observeMillis) throws InterruptedException {
        LinkedTransferQueue<String> queue = new LinkedTransferQueue<>();
        CountDownLatch transferReturned = new CountDownLatch(1);

        Thread producer = new Thread(() -> {
            try {
                queue.transfer("only-item");
                transferReturned.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "transfer-producer");
        producer.setDaemon(true);
        producer.start();

        // Nobody has taken the item, so transfer must still be waiting.
        boolean blockedWhileUnclaimed = !transferReturned.await(observeMillis, TimeUnit.MILLISECONDS);

        // Take it, and the producer is released.
        String received = queue.poll(10, TimeUnit.SECONDS);
        boolean released = transferReturned.await(10, TimeUnit.SECONDS);
        producer.join(TimeUnit.SECONDS.toMillis(5));

        return blockedWhileUnclaimed && "only-item".equals(received) && released;
    }

    /**
     * Producer that uses transfer() for synchronous handoff
     */
    static class TransferProducer implements Runnable {
        private final LinkedTransferQueue<String> queue;
        private final int producerId;
        private final int itemCount;
        private final AtomicInteger producedCount;

        public TransferProducer(LinkedTransferQueue<String> queue, int producerId, int itemCount,
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
                    producedCount.incrementAndGet();
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
        private final AtomicInteger consumedCount;
        private final CountDownLatch allConsumed;

        public TransferConsumer(LinkedTransferQueue<String> queue, int consumerId,
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
                    System.out.println("Consumer " + consumerId + " waiting for item...");
                    String item = queue.poll(3, TimeUnit.SECONDS);
                    if (item != null) {
                        consumedCount.incrementAndGet();
                        allConsumed.countDown();
                        System.out.println("Consumer " + consumerId + " received: " + item);
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