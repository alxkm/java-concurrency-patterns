package org.alxkm.patterns.atomics;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Demonstrates the usage of AtomicReferenceFieldUpdater in a multithreaded environment.
 * <p>
 * AtomicReferenceFieldUpdater provides atomic operations on reference fields of objects.
 */
public class AtomicReferenceFieldUpdaterExample {

    // Define a class with a reference field
    static class MyClass {
        volatile String value;
    }

    /**
     * MyClass is a simple class with a volatile reference field named value.
     * An AtomicReferenceFieldUpdater named updater is created for the value field of MyClass.
     * Two threads are created: an updater thread and a reader thread.
     * The updater thread updates the value field of myObject using updater.set() method.
     * The reader thread reads the value field of myObject using updater.get() method.
     * Both threads run concurrently.
     * After both threads have completed, the main thread prints the final state of the field.
     */
    public static void main(String[] args) {
        // Create an AtomicReferenceFieldUpdater for the 'value' field of MyClass
        AtomicReferenceFieldUpdater<MyClass, String> updater =
                AtomicReferenceFieldUpdater.newUpdater(MyClass.class, String.class, "value");

        // Create an instance of MyClass
        MyClass myObject = new MyClass();

        // Define worker threads
        Thread updaterThread = new Thread(() -> {
            // Update the value using AtomicReferenceFieldUpdater
            updater.set(myObject, "New Value");
            System.out.println("Updater Thread updated value to: " + updater.get(myObject));
        });

        Thread readerThread = new Thread(() -> {
            // Read the value
            String value = updater.get(myObject);
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

