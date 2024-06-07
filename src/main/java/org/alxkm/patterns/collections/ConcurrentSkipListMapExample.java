package org.alxkm.patterns.collections;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Demonstrates the usage of ConcurrentSkipListMap in a multithreaded environment.
 * Where multiple threads can safely modify the map without explicit synchronization.
 */
public class ConcurrentSkipListMapExample {

    /**
     * We create a ConcurrentSkipListMap named skipListMap.
     * We put some initial key-value pairs into the map.
     * We print the initial state of the map.
     * We create two threads (thread1 and thread2) to concurrently modify the map by adding new key-value pairs.
     * Each thread adds a new key-value pair to the map.
     * We start both threads and wait for them to finish using the join() method.
     * Finally, we print the final state of the map after the threads have finished executing.
     */
    public static void main(String[] args) {
        // Create a ConcurrentSkipListMap
        ConcurrentSkipListMap<Integer, String> skipListMap = new ConcurrentSkipListMap<>();

        // Put elements into the map
        skipListMap.put(3, "Apple");
        skipListMap.put(1, "Banana");
        skipListMap.put(5, "Orange");

        // Print the initial map
        System.out.println("Initial Map: " + skipListMap);

        // Create and start threads to modify the map concurrently
        Thread thread1 = new Thread(() -> {
            skipListMap.put(2, "Mango");
            System.out.println("Thread 1 added (2, Mango)");
        });

        Thread thread2 = new Thread(() -> {
            skipListMap.put(4, "Pineapple");
            System.out.println("Thread 2 added (4, Pineapple)");
        });

        thread1.start();
        thread2.start();

        try {
            // Wait for threads to finish
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Print the final map
        System.out.println("Final Map: " + skipListMap);
    }
}

