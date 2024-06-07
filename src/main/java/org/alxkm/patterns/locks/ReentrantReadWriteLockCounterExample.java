package org.alxkm.patterns.locks;

/**
 * An example demonstrating the usage of ReentrantReadWriteLockCounter to manage a shared counter
 * among multiple threads.
 */
public class ReentrantReadWriteLockCounterExample {
    /**
     * The main method of the example program.
     *
     * @param args The command-line arguments (unused).
     */
    public static void main(String[] args) {
        ReentrantReadWriteLockCounter counter = new ReentrantReadWriteLockCounter(); // Create a new ReentrantReadWriteLockCounter instance
        int numThreads = 5; // Number of threads
        Thread[] threads = new Thread[numThreads]; // Array to hold threads

        // Create and start multiple threads to increment the counter
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.incrementCounter(); // Increment the counter in a loop
                }
            });
            threads[i].start(); // Start the thread
        }

        // Wait for all threads to finish execution
        try {
            for (Thread thread : threads) {
                thread.join(); // Wait for each thread to finish
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Retrieve the final value of the counter and print it
        int finalCounterValue = counter.getCounter();
        System.out.println("Final Counter Value: " + finalCounterValue);
    }
}
