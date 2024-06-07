package org.alxkm.patterns.atomics;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Demonstrates the usage of AtomicLongFieldUpdater in a multithreaded environment.
 * <p>
 * AtomicLongFieldUpdater provides atomic operations on long fields of objects.
 */
public class AtomicLongFieldUpdaterExample {

    // Define a class with a long field
    static class MyClass {
        volatile long value;
    }

    /**
     * MyClass is a simple class with a volatile long field named value.
     * An AtomicLongFieldUpdater named updater is created for the value field of MyClass.
     * Two threads are created: an updater thread and a reader thread.
     * The updater thread updates the value field of myObject using updater.set() method.
     * The reader thread reads the value field of myObject using updater.get() method.
     * Both threads run concurrently.
     * After both threads have completed, the main thread prints the final state of the field.
     */
    public static void main(String[] args) {
        // Create an AtomicLongFieldUpdater for the 'value' field of MyClass
        AtomicLongFieldUpdater<MyClass> updater = AtomicLongFieldUpdater.newUpdater(MyClass.class, "value");

        // Create an instance of MyClass
        MyClass myObject = new MyClass();

        // Define worker threads
        Thread updaterThread = new Thread(() -> {
            // Update the value using AtomicLongFieldUpdater
            updater.set(myObject, 10);
            System.out.println("Updater Thread updated value to: " + updater.get(myObject));
        });

        Thread readerThread = new Thread(() -> {
            // Read the value
            long value = updater.get(myObject);
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

