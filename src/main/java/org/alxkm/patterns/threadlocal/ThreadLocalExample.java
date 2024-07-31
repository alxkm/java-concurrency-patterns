package org.alxkm.patterns.threadlocal;

/**
 * Provides each thread with its own instance of a class.
 * The ThreadSpecificStorageExample class demonstrates the use of ThreadLocal to give each thread its own instance of a variable.
 */
public class ThreadLocalExample {
    // ThreadLocal variable to store an Integer instance for each thread
    public static final ThreadLocal<Integer> THREAD_LOCAL = ThreadLocal.withInitial(() -> 0);

    /**
     * The main method of the example program.
     *
     * @param args The command-line arguments (unused).
     */
    public static void main(String[] args) {
        // Create and start the first thread
        Thread t1 = new Thread(() -> {
            THREAD_LOCAL.set(1); // Set the ThreadLocal value for this thread
            System.out.println("Thread 1: " + THREAD_LOCAL.get()); // Print the ThreadLocal value
        });

        // Create and start the second thread
        Thread t2 = new Thread(() -> {
            THREAD_LOCAL.set(2); // Set the ThreadLocal value for this thread
            System.out.println("Thread 2: " + THREAD_LOCAL.get()); // Print the ThreadLocal value
        });

        t1.start(); // Start the first thread
        t2.start(); // Start the second thread
    }
}

