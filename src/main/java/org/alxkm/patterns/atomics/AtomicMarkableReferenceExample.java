package org.alxkm.patterns.atomics;

import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Demonstrates the usage of AtomicMarkableReference in a multithreaded environment.
 */
public class AtomicMarkableReferenceExample {

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
     * Demonstrates how AtomicMarkableReference ensures atomic updates of a shared reference
     * along with its mark in a multithreaded environment without requiring explicit synchronization.
     *
     *
     * The SharedResource class represents a shared object that contains some data.
     * The main method creates an AtomicMarkableReference initialized with an instance of SharedResource and a mark.
     * Two threads are created: an updater thread and a reader thread.
     * The updater thread sets a new instance of SharedResource with updated data using atomicMarkableReference.set() and sets the mark using atomicMarkableReference.attemptMark().
     * The reader thread reads the current value of the shared resource along with its mark using atomicMarkableReference.get().
     * Both threads run concurrently.
     * After both threads have completed, the main thread prints the final state of the shared resource along with its mark.
     */
    public static void main(String[] args) {
        SharedResource initialResource = new SharedResource("Initial Data");
        AtomicMarkableReference<SharedResource> atomicMarkableReference = new AtomicMarkableReference<>(initialResource, false);

        // Define worker threads
        Thread updaterThread = new Thread(() -> {
            SharedResource newResource = new SharedResource("New Data");
            boolean[] mark = {false};
            atomicMarkableReference.attemptMark(initialResource, true);
            atomicMarkableReference.set(newResource, false);
            System.out.println(Thread.currentThread().getName() + " sets new data: " + newResource.getData());
        });

        Thread readerThread = new Thread(() -> {
            boolean[] mark = {false};
            SharedResource currentResource = atomicMarkableReference.get(mark);
            System.out.println(Thread.currentThread().getName() + " reads data: " + currentResource.getData() + ", Mark: " + mark[0]);
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

        // Final result
        boolean[] mark = {false};
        SharedResource finalResource = atomicMarkableReference.get(mark);
        System.out.println("Final data: " + finalResource.getData() + ", Mark: " + mark[0]);
    }
}
