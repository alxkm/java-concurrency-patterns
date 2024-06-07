package org.alxkm.patterns.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The ProducerConsumerExample class demonstrates the producer-consumer pattern, where one thread
 * produces data and another thread consumes it through a shared buffer.
 */
public class ProducerConsumerBlockingQueueExample {
    /**
     * The main method creates a shared blocking queue and starts producer and consumer threads.
     */
    public static void main(String[] args) {
        BlockingQueue<Integer> sharedQueue = new LinkedBlockingQueue<>();

        Thread producerThread = new Thread(new Producer(sharedQueue));
        Thread consumerThread = new Thread(new Consumer(sharedQueue));

        producerThread.start();
        consumerThread.start();
    }
}

/**
 * The Producer class represents a producer thread that adds data to a shared blocking queue.
 */
class Producer implements Runnable {
    private final BlockingQueue<Integer> sharedQueue;

    /**
     * Constructs a Producer with the specified shared queue.
     *
     * @param sharedQueue the shared blocking queue
     */
    public Producer(BlockingQueue<Integer> sharedQueue) {
        this.sharedQueue = sharedQueue;
    }

    /**
     * The run method of the producer thread adds integers to the shared queue.
     */
    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            try {
                System.out.println("Produced: " + i);
                sharedQueue.put(i);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

/**
 * The Consumer class represents a consumer thread that retrieves data from a shared blocking queue.
 */
class Consumer implements Runnable {
    private final BlockingQueue<Integer> sharedQueue;

    /**
     * Constructs a Consumer with the specified shared queue.
     *
     * @param sharedQueue the shared blocking queue
     */
    public Consumer(BlockingQueue<Integer> sharedQueue) {
        this.sharedQueue = sharedQueue;
    }

    /**
     * The run method of the consumer thread retrieves integers from the shared queue.
     */
    @Override
    public void run() {
        while (true) {
            try {
                Integer item = sharedQueue.take();
                System.out.println("Consumed: " + item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
