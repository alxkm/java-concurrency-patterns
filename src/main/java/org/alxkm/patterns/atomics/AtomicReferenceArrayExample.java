package org.alxkm.patterns.atomics;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Demonstrates the usage of AtomicReferenceArray in a multithreaded environment.
 * AtomicReferenceArray provides atomic operations on arrays of reference values.
 */
public class AtomicReferenceArrayExample {
    /**
     * An AtomicReferenceArray named array is created with an initial size of 3 and populated with initial values.
     * The updater thread updates the value at index 1 of the array using the compareAndSet method, which atomically compares the current value with an expected value and sets the new value if the comparison is successful.
     * The reader thread simply reads the value at index 1 of the array.
     * Both threads run concurrently.
     * After both threads have completed, the main thread prints the final state of the array.
     */
    public static void main(String[] args) {
        // Create an AtomicReferenceArray with initial values
        AtomicReferenceArray<String> array = new AtomicReferenceArray<>(3);
        array.set(0, "Value 1");
        array.set(1, "Value 2");
        array.set(2, "Value 3");

        // Define worker threads
        Thread updaterThread = new Thread(() -> {
            // Update the value at index 1
            array.compareAndSet(1, "Value 2", "New Value 2");
            System.out.println("Updater Thread updated value at index 1 to: " + array.get(1));
        });

        Thread readerThread = new Thread(() -> {
            // Read the value at index 1
            String value = array.get(1);
            System.out.println("Reader Thread read value at index 1: " + value);
        });

        // Start worker threads
        updaterThread.start();
        readerThread.start();

        // Wait for worker threads to complete
        try {
            updaterThread.join();
            readerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

