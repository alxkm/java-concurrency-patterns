package org.alxkm.patterns.atomics;

import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Demonstrates the usage of AtomicStampedReference in a multithreaded environment.
 * <p>
 * AtomicStampedReference allows atomically updating a reference along with an integer stamp.
 * This example involves multiple threads trying to update a shared reference along with its stamp atomically.
 */
public class AtomicStampedReferenceExample {

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
     * Demonstrates how AtomicStampedReference ensures atomic updates of a shared reference along with its stamp
     * in a multithreaded environment without requiring explicit synchronization.
     *
     *
     * The SharedResource class represents a shared object that contains some data.
     * The main method creates an AtomicStampedReference initialized with an instance of SharedResource and a stamp.
     * Two threads are created: an updater thread and a reader thread.
     * The updater thread sets a new instance of SharedResource with updated data using atomicStampedReference.set() and sets the stamp.
     * The reader thread reads the current value of the shared resource along with its stamp using atomicStampedReference.get().
     * Both threads run concurrently.
     * After both threads have completed, the main thread prints the final state of the shared resource along with its stamp.
     */
    public static void main(String[] args) {
        SharedResource initialResource = new SharedResource("Initial Data");
        AtomicStampedReference<SharedResource> atomicStampedReference = new AtomicStampedReference<>(initialResource, 0);

        // Define worker threads
        Thread updaterThread = new Thread(() -> {
            SharedResource newResource = new SharedResource("New Data");
            int[] stamp = {0};
            atomicStampedReference.set(newResource, 1);
            System.out.println(Thread.currentThread().getName() + " sets new data: " + newResource.getData() + ", Stamp: " + stamp[0]);
        });

        Thread readerThread = new Thread(() -> {
            int[] stamp = {0};
            SharedResource currentResource = atomicStampedReference.get(stamp);
            System.out.println(Thread.currentThread().getName() + " reads data: " + currentResource.getData() + ", Stamp: " + stamp[0]);
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
        int[] stamp = {0};
        SharedResource finalResource = atomicStampedReference.get(stamp);
        System.out.println("Final data: " + finalResource.getData() + ", Stamp: " + stamp[0]);
    }
}
