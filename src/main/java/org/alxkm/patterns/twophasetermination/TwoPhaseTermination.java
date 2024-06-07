package org.alxkm.patterns.twophasetermination;

/**
 * Provides a graceful way to terminate threads, ensuring that resources are properly released.
 * The TwoPhaseTermination class demonstrates a thread termination mechanism that allows for cleanup
 * before the thread fully terminates.
 */
public class TwoPhaseTermination extends Thread {
    private volatile boolean running = true; // Flag to control the running state of the thread

    /**
     * The run method contains the code to be executed by the thread.
     * It runs in a loop, simulating work by sleeping for 100 milliseconds, until the running flag is set to false.
     * If interrupted, it sets the interrupt flag again and proceeds to the cleanup.
     */
    @Override
    public void run() {
        try {
            while (running) {
                // Simulate work
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
        } finally {
            // Cleanup
            cleanup(); // Perform cleanup actions
        }
    }

    /**
     * Terminates the thread by setting the running flag to false and interrupting the thread.
     * This method allows the thread to exit the loop and perform cleanup before terminating.
     */
    public void terminate() {
        running = false; // Set the running flag to false to exit the loop
        interrupt(); // Interrupt the thread if it is sleeping or waiting
    }

    /**
     * Performs cleanup actions before the thread terminates.
     * This method is called in the finally block of the run method to ensure that resources are properly released.
     */
    private void cleanup() {
        System.out.println("Cleaning up resources...");
    }
}