package org.alxkm.antipatterns.lockcontention;

/**
 *
 * Lock contention occurs when multiple threads compete for the same lock, causing some threads to wait while another thread holds the lock.
 * This can lead to reduced performance due to increased waiting times and underutilized CPU resources.
 *
 *
 * In this example, both increment and getCounter methods synchronize on the same lock.
 * When multiple threads try to access these methods simultaneously, they experience high contention, reducing performance.
 *
 */
public class LockContentionExample {
    private final Object lock = new Object();
    private int counter = 0;

    /**
     * Increments the counter. This method synchronizes on the same lock,
     * leading to high contention when accessed by multiple threads.
     */
    public void increment() {
        synchronized (lock) {
            counter++;
        }
    }

    /**
     * Returns the counter value.
     * This method also synchronizes on the same lock, contributing to contention.
     *
     * @return the counter value.
     */
    public int getCounter() {
        synchronized (lock) {
            return counter;
        }
    }

    public static void main(String[] args) {
        LockContentionExample lockContentionExample = new LockContentionExample();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                lockContentionExample.increment();
            }
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Final counter value: " + lockContentionExample.getCounter());
    }
}

