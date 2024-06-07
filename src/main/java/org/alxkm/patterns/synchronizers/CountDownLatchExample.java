package org.alxkm.patterns.synchronizers;

import java.util.concurrent.CountDownLatch;

/**
 * The CountDownLatchExample class demonstrates the usage of CountDownLatch, which is a synchronization aid that allows
 * one or more threads to wait until a set of operations being performed in other threads completes. It is useful for
 * scenarios where you want a thread to wait for other threads to complete certain tasks before proceeding.
 */
public class CountDownLatchExample {
    private final CountDownLatch latch;

    /**
     * Constructs a CountDownLatchExample with the given count.
     *
     * @param count The number of times the {@link #performTask()} method must be invoked before threads waiting
     *              at the latch are released.
     */
    public CountDownLatchExample(int count) {
        this.latch = new CountDownLatch(count);
    }

    /**
     * Simulates the completion of a task and decrements the latch count.
     */
    public void performTask() {
        // Simulate task completion
        System.out.println(Thread.currentThread().getName() + " completed a task.");
        latch.countDown();
    }

    /**
     * Causes the current thread to wait until the latch count reaches zero, indicating that all tasks have completed.
     *
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    public void waitForCompletion() throws InterruptedException {
        latch.await();
        System.out.println("All tasks completed. Proceeding...");
    }

    /**
     * Gets the CountDownLatch associated with this example.
     *
     * @return The CountDownLatch object.
     */
    public CountDownLatch getLatch() {
        return latch;
    }
}

