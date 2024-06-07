package org.alxkm.antipatterns.busywaiting;

/**
 * Demonstrates the resolution of busy waiting by using the wait() and notify() methods for thread synchronization.
 */
public class BusyWaitingResolution {

    private boolean flag = false;

    public static void main(String[] args) throws InterruptedException {
        BusyWaitingResolution example = new BusyWaitingResolution();
        Thread workerThread = new Thread(example::doWork);
        workerThread.start();

        // Simulate some work in the main thread
        Thread.sleep(2000);

        // Set the flag to true and notify the worker thread to proceed
        synchronized (example) {
            example.flag = true;
            example.notify();
        }

        workerThread.join();
    }

    /**
     * Uses the wait() method to avoid busy waiting.
     */
    public synchronized void doWork() {
        while (!flag) {
            try {
                // Wait for the flag to be set
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Flag is set. Proceeding with work...");
    }
}
