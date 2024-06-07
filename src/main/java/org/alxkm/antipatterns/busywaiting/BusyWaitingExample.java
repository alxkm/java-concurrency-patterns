package org.alxkm.antipatterns.busywaiting;

/**
 * Demonstrates busy waiting by continuously checking a condition in a loop without yielding control of the CPU.
 */
public class BusyWaitingExample {

    private volatile boolean flag = false;

    public static void main(String[] args) throws InterruptedException {
        BusyWaitingExample example = new BusyWaitingExample();
        Thread workerThread = new Thread(example::doWork);
        workerThread.start();

        // Simulate some work in the main thread
        Thread.sleep(2000);

        // Set the flag to true to signal the worker thread to proceed
        example.flag = true;

        workerThread.join();
    }

    /**
     * Continuously checks the flag in a loop, causing busy waiting.
     */
    public void doWork() {
        while (!flag) {
            // Busy wait
        }
        System.out.println("Flag is set. Proceeding with work...");
    }
}
