package org.alxkm.patterns.locks;

import java.util.concurrent.locks.LockSupport;

/**
 * Demonstrates the use of LockSupport for thread synchronization.
 *
 * LockSupportExample Class: The main class demonstrating the use of LockSupport.
 * Task Class: A nested static class implementing Runnable, which parks its thread and waits to be unparked.
 * run() Method: Parks the thread using LockSupport.park() and waits to be unparked.
 * main() Method:
 * taskThread: A new thread running an instance of Task that parks itself.
 * Thread.sleep(1000): Simulates some work in the main thread before unparking the taskThread.
 * LockSupport.unpark(taskThread): Unparks the taskThread, allowing it to continue execution.
 */
public class LockSupportExample {

    /**
     * A task that parks the current thread and waits to be unparked.
     */
    private static class Task implements Runnable {
        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " is about to park.");
            LockSupport.park();
            System.out.println(Thread.currentThread().getName() + " is unparked and continues execution.");
        }
    }

    /**
     * Main method that demonstrates parking and unparking of threads.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        // Create and start a new thread that will park itself
        Thread taskThread = new Thread(new Task(), "TaskThread");
        taskThread.start();

        try {
            Thread.sleep(1000); // Simulate some work in the main thread
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Unpark the parked thread
        System.out.println("Main thread is about to unpark TaskThread.");
        LockSupport.unpark(taskThread);
    }
}

