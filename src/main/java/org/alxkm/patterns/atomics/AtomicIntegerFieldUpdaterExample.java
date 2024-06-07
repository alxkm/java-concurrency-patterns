package org.alxkm.patterns.atomics;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Demonstrates the usage of AtomicIntegerFieldUpdater in a multithreaded environment.
 * <p>
 * AtomicIntegerFieldUpdater provides atomic operations on integer fields of objects.
 */
public class AtomicIntegerFieldUpdaterExample {

    // Define a class with an integer field
    static class MyClass {
        volatile int value;
    }

    /**
     * MyClass is a simple class with a volatile integer field named value.
     * An AtomicIntegerFieldUpdater named updater is created for the value field of MyClass.
     * Two threads are created: an updater thread and a reader thread.
     * The updater thread updates the value field of myObject using updater.set() method.
     * The reader thread reads the value field of myObject using updater.get() method.
     * Both threads run concurrently.
     * After both threads have completed, the main thread prints the final state of the field.
     */
    public static void main(String[] args) {
        // Create an AtomicIntegerFieldUpdater for the 'value' field of MyClass
        AtomicIntegerFieldUpdater<MyClass> updater = AtomicIntegerFieldUpdater.newUpdater(MyClass.class, "value");

        // Create an instance of MyClass
        MyClass myObject = new MyClass();

        // Define worker threads
        Thread updaterThread = new Thread(() -> {
            // Update the value using AtomicIntegerFieldUpdater
            updater.set(myObject, 10);
            System.out.println("Updater Thread updated value to: " + updater.get(myObject));
        });

        Thread readerThread = new Thread(() -> {
            // Read the value
            int value = updater.get(myObject);
            System.out.println("Reader Thread read value: " + value);
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
