package ua.com.alxkm.patterns.blockingqueue;

import java.util.concurrent.BlockingQueue;

/**
 * The Consumer class represents a consumer thread that consumes messages from a blocking queue.
 * It continuously takes messages from the queue and prints them until interrupted.
 */
public class Consumer implements Runnable {
    protected BlockingQueue<String> queue;

    /**
     * Constructs a Consumer with the specified blocking queue.
     *
     * @param queue The blocking queue from which messages will be consumed.
     */
    public Consumer(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    /**
     * The run method of the Consumer thread. It continuously takes messages from the blocking queue and prints them
     * until interrupted.
     */
    @Override
    public void run() {
        try {
            System.out.println(queue.take());
            System.out.println(queue.take());
            System.out.println(queue.take());
            System.out.println(queue.take());
            System.out.println(queue.take());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}