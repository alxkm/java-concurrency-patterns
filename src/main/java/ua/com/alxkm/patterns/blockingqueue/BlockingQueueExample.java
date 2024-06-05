package ua.com.alxkm.patterns.blockingqueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * The BlockingQueueExample class demonstrates the usage of a blocking queue to safely pass messages between producer
 * and consumer threads. It consists of a producer thread that produces messages and puts them into the blocking queue,
 * and a consumer thread that consumes messages from the queue.
 */
public class BlockingQueueExample {
    /**
     * The main method creates a blocking queue with a capacity of 1024, a producer, and a consumer. It starts both
     * threads and then sleeps for 4 seconds to allow the producer and consumer to execute.
     *
     * @param args Command-line arguments (not used).
     * @throws InterruptedException If the main thread is interrupted while sleeping.
     */
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(1024);
        Producer producer = new Producer(queue);
        Consumer consumer = new Consumer(queue);
        new Thread(producer).start();
        new Thread(consumer).start();
        Thread.sleep(4000);
    }
}