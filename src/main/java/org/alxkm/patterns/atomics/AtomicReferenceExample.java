package org.alxkm.patterns.atomics;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Demonstrates the usage of AtomicReference in a multithreaded environment.
 */
public class AtomicReferenceExample {

    static class SharedResource {
        private String data;

        public SharedResource(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    /**
     *
     * Demonstrates how AtomicReference ensures atomic updates of a shared reference in a multithreaded environment
     * without requiring explicit synchronization.
     *
     * The SharedResource class represents a shared object that contains some data.
     * The main method creates an AtomicReference initialized with an instance of SharedResource.
     * Two threads are created: a writer thread and a reader thread.
     * The writer thread sets a new instance of SharedResource with updated data using atomicReference.set().
     * The reader thread reads the current value of the shared resource using atomicReference.get().
     * Both threads run concurrently.
     * After both threads have completed, the main thread prints the final state of the shared resource.
     *
     */
    public static void main(String[] args) {
        SharedResource initialResource = new SharedResource("Initial Data");
        AtomicReference<SharedResource> atomicReference = new AtomicReference<>(initialResource);

        // Define worker threads
        Thread writerThread = new Thread(() -> {
            SharedResource newResource = new SharedResource("New Data");
            atomicReference.set(newResource);
            System.out.println(Thread.currentThread().getName() + " sets new data: " + newResource.getData());
        });

        Thread readerThread = new Thread(() -> {
            SharedResource currentResource = atomicReference.get();
            System.out.println(Thread.currentThread().getName() + " reads data: " + currentResource.getData());
        });

        // Start worker threads
        writerThread.start();
        readerThread.start();

        // Wait for worker threads to complete
        try {
            writerThread.join();
            readerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Final result
        SharedResource finalResource = atomicReference.get();
        System.out.println("Final data: " + finalResource.getData());
    }
}
