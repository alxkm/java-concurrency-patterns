package org.alxkm.patterns.queue;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomBlockingQueueTest {

    /**
     * This test method verifies the functionality of the CustomBlockingQueue class, which implements
     * a blocking queue with a fixed capacity. It creates a CustomBlockingQueue instance with a capacity
     * of 5 and launches multiple producer and consumer threads. Each producer thread enqueues an item
     * into the queue, while each consumer thread dequeues an item from the queue. The test ensures that
     * all producer and consumer threads complete their operations by using CountDownLatch objects to
     * synchronize their execution. After all producer and consumer threads have finished, the test checks
     * that the size of the queue is 0, indicating that all items have been dequeued and the queue is empty.
     */
    @Test
    void testBlockingQueue() throws InterruptedException {
        final int capacity = 5;
        final CustomBlockingQueue<Integer> blockingQueue = new CustomBlockingQueue<>(capacity);
        final int numThreads = 10;

        final CountDownLatch producerLatch = new CountDownLatch(numThreads);
        final CountDownLatch consumerLatch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    blockingQueue.enqueue(1);
                    producerLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    blockingQueue.dequeue();
                    consumerLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        producerLatch.await();
        consumerLatch.await();

        assertEquals(0, blockingQueue.size(), "Queue should be empty after all items are dequeued");
    }
}