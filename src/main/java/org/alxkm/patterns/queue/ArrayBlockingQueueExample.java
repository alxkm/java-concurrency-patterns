package org.alxkm.patterns.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * The ArrayBlockingQueueExample class demonstrates the usage of a blocking queue, specifically an ArrayBlockingQueue.
 * It provides methods to produce and consume elements from the queue.
 */
public class ArrayBlockingQueueExample {
    private final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

    /**
     * Produces an integer value and puts it into the blocking queue.
     *
     * @param value The value to be produced and put into the queue.
     * @throws InterruptedException If the thread is interrupted while waiting to put the value into the queue.
     */
    public void produce(int value) throws InterruptedException {
        queue.put(value);
    }

    /**
     * Consumes an integer value from the blocking queue.
     *
     * @return The consumed value from the queue.
     * @throws InterruptedException If the thread is interrupted while waiting to take a value from the queue.
     */
    public Integer consume() throws InterruptedException {
        return queue.take();
    }
}

