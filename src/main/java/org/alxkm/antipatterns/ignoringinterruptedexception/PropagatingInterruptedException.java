package org.alxkm.antipatterns.ignoringinterruptedexception;

/**
 * Another approach is to propagate the InterruptedException up the call stack,
 * allowing higher-level methods to handle it appropriately.
 *
 *
 * In this version, the loop condition checks for Thread.currentThread().isInterrupted(),
 * ensuring that the loop exits when the thread is interrupted.
 * The InterruptedException is caught and handled by logging a message and exiting the loop.
 *
 */
public class PropagatingInterruptedException implements Runnable {
    /**
     * The run method performs a long-running task.
     * It propagates InterruptedException by declaring it in the method signature.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Simulating long-running task
                Thread.sleep(1000);
                System.out.println("Working...");
            }
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted, stopping.");
        }
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new PropagatingInterruptedException());
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

