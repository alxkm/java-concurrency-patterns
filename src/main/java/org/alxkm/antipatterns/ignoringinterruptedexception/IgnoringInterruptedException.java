package org.alxkm.antipatterns.ignoringinterruptedexception;

/**
 * Ignoring InterruptedException is a common problem in Java concurrency where threads that are interrupted do not properly handle the interruption,
 * leading to issues like thread leaks, unresponsive applications, or improper termination of tasks. Here's an example demonstrating this problem along with a resolution.
 *
 * In this example, the run method catches InterruptedException but does not handle it appropriately,
 * simply ignoring it. This can cause the thread to continue running even when it should stop.
 *
 */
public class IgnoringInterruptedException implements Runnable {
    /**
     * The run method performs a long-running task.
     * It ignores InterruptedException, which is a bad practice.
     */
    @Override
    public void run() {
        while (true) {
            try {
                // Simulating long-running task
                Thread.sleep(1000);
                System.out.println("Working...");
            } catch (InterruptedException e) {
                // Ignoring the interruption, which is bad practice
            }
        }
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new IgnoringInterruptedException());
        thread.start();

        // Interrupt the thread after 3 seconds
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        thread.interrupt();
    }
}

