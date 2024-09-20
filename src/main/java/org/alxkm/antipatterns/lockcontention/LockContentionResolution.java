package org.alxkm.antipatterns.lockcontention;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * To resolve lock contention, we can reduce the granularity of the locks, minimizing the critical sections that need synchronization.
 *
 * In this revised example, we use AtomicInteger to manage the counter.
 * AtomicInteger provides thread-safe operations without the need for explicit synchronization, significantly reducing lock contention.
 *
 */
public class LockContentionResolution {
    private final AtomicInteger counter = new AtomicInteger();

    /**
     * Increments the counter using an atomic operation, reducing the need for explicit locks.
     */
    public void increment() {
        counter.incrementAndGet();
    }

    /**
     * Returns the counter value. This method is thread-safe without explicit locks.
     *
     * @return the counter value.
     */
    public int getCounter() {
        return counter.get();
    }

    public static void main(String[] args) {
        LockContentionResolution lockContentionResolution = new LockContentionResolution();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                lockContentionResolution.increment();
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

        System.out.println("Final counter value: " + lockContentionResolution.getCounter());
    }
}

