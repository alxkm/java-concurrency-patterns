package org.alxkm.patterns.queue;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Demonstrates the usage of ConcurrentLinkedQueue in a multithreaded environment.
 */
public class ConcurrentLinkedQueueExample {
    /**
     *
     * We create a ConcurrentLinkedQueue named queue.
     * We define two producer threads (producer1 and producer2) that add elements to the queue using the offer() method.
     * Each producer thread adds elements to the queue with a delay of 1 second between each addition.
     * We define two consumer threads (consumer1 and consumer2) that remove elements from the queue using the poll() method.
     * Each consumer thread removes elements from the queue with a delay of 2 seconds between each removal.
     * We start all producer and consumer threads concurrently.
     * As elements are added and removed from the queue, the producer and consumer threads print messages indicating the elements they add or remove.
     *
     */
    public static void main(String[] args) {
        // Create a ConcurrentLinkedQueue
        ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();

        // Create and start producer threads
        Thread producer1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                queue.offer(i); // Add elements to the queue
                System.out.println("Producer 1 added: " + i);
                try {
                    Thread.sleep(1000); // Simulate production time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread producer2 = new Thread(() -> {
            for (int i = 5; i < 10; i++) {
                queue.offer(i); // Add elements to the queue
                System.out.println("Producer 2 added: " + i);
                try {
                    Thread.sleep(1000); // Simulate production time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // Create and start consumer threads
        Thread consumer1 = new Thread(() -> {
            while (!queue.isEmpty()) {
                int element = queue.poll(); // Remove and return the first element from the queue
                System.out.println("Consumer 1 removed: " + element);
                try {
                    Thread.sleep(2000); // Simulate consumption time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread consumer2 = new Thread(() -> {
            while (!queue.isEmpty()) {
                int element = queue.poll(); // Remove and return the first element from the queue
                System.out.println("Consumer 2 removed: " + element);
                try {
                    Thread.sleep(2000); // Simulate consumption time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // Start producer and consumer threads
        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();
    }
}
