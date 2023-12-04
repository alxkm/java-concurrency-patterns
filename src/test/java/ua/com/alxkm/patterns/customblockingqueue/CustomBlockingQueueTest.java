package ua.com.alxkm.patterns.customblockingqueue;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomBlockingQueueTest {

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