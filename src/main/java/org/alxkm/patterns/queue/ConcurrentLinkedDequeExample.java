package org.alxkm.patterns.queue;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Demonstrates the usage of ConcurrentLinkedDeque in a multithreaded environment.
 * <p>
 * Where multiple threads can safely add and remove elements from the deque without explicit synchronization.
 */
public class ConcurrentLinkedDequeExample {

    /**
     * We create a ConcurrentLinkedDeque named deque.
     * We define two producer threads (producer1 and producer2) that add elements to the deque.
     * Each producer thread adds elements to the deque with a delay of 1 second between each addition.
     * We define two consumer threads (consumer1 and consumer2) that remove elements from the deque.
     * consumer1 removes elements from the front of the deque, while consumer2 removes elements from the end of the deque.
     * Each consumer thread removes elements from the deque with a delay of 2 seconds between each removal.
     * We start all producer and consumer threads concurrently.
     * As elements are added and removed from the deque, the producer and consumer threads print messages indicating the elements they add or remove.
     */
    public static void main(String[] args) {
        // Create a ConcurrentLinkedDeque
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();

        // Create and start producer threads
        Thread producer1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                deque.add(i); // Add elements to the deque
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
                deque.add(i); // Add elements to the deque
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
            while (!deque.isEmpty()) {
                int element = deque.poll(); // Remove and return the first element from the deque
                System.out.println("Consumer 1 removed: " + element);
                try {
                    Thread.sleep(2000); // Simulate consumption time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread consumer2 = new Thread(() -> {
            while (!deque.isEmpty()) {
                int element = deque.pollLast(); // Remove and return the last element from the deque
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
