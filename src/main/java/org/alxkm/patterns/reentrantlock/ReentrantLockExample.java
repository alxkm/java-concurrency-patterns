package org.alxkm.patterns.reentrantlock;

/**
 * An example demonstrating the usage of ReentrantLockCounter to manage a shared counter
 * among multiple threads in a multithreaded environment.
 */
public class ReentrantLockExample {
    /**
     * The main method of the example program.
     *
     * @param args The command-line arguments (unused).
     * @throws InterruptedException If any thread is interrupted while sleeping.
     */
    public static void main(String[] args) throws InterruptedException {
        ReentrantLockCounter reentrantLockCounter = new ReentrantLockCounter(); // Create a new ReentrantLockCounter instance

        // Create and start multiple threads to increment the counter concurrently
        for (int i = 0; i < 100; i++) {
            new Thread(reentrantLockCounter::incrementCounter).start(); // Start a new thread to increment the counter
        }

        // Sleep for 5 seconds to allow all threads to finish execution
        Thread.sleep(5000);

        // Retrieve and print the final value of the counter
        System.out.println(reentrantLockCounter.getCounter());
    }
}
