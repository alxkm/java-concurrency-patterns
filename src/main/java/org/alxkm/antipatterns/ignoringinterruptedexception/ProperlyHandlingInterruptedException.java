package org.alxkm.antipatterns.ignoringinterruptedexception;

/**
 *
 * To resolve this issue, the thread should properly handle InterruptedException by either propagating the exception or breaking out of the loop.
 *
 * In this revised example, the run method handles the
 * InterruptedException by restoring the interrupt status with Thread.currentThread().interrupt() and breaking out of the loop.
 * This ensures that the thread stops running as intended.
 *
 */
public class ProperlyHandlingInterruptedException implements Runnable {
    /**
     * The run method performs a long-running task.
     * It handles InterruptedException properly by restoring the interrupt status.
     */
    @Override
    public void run() {
        while (true) {
            try {
                // Simulating long-running task
                Thread.sleep(1000);
                System.out.println("Working...");
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted, stopping.");
                break;
            }
        }
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new ProperlyHandlingInterruptedException());
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

