package org.alxkm.patterns.collections;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class demonstrates the usage of a blocking queue in a producer-consumer scenario.
 * A blocking queue with a capacity of 5 is created using ArrayBlockingQueue.
 * A producer thread is created, which continuously adds elements to the blocking queue.
 * The producer puts elements into the queue using the put() method. If the queue is full,
 * the put() method blocks until space becomes available.
 * A consumer thread is created, which continuously removes elements from the blocking queue.
 * The consumer takes elements from the queue using the take() method. If the queue is empty,
 * the take() method blocks until elements become available.
 * Both producer and consumer threads run in parallel, simulating the production and consumption
 * of elements from the blocking queue. Elements produced are printed with "Produced" prefix,
 * and elements consumed are printed with "Consumed" prefix, along with their respective values.
 */
public class BlockingQueueSimpleExample {
    public static void main(String[] args) {
        // Create a blocking queue with a capacity of 5
        BlockingQueue<Integer> blockingQueue = new ArrayBlockingQueue<>(5);

        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    // Put elements into the queue
                    blockingQueue.put(i);
                    System.out.println("Produced: " + i);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    // Take elements from the queue
                    int value = blockingQueue.take();
                    System.out.println("Consumed: " + value);
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Start the producer and consumer threads
        producer.start();
        consumer.start();
    }
}