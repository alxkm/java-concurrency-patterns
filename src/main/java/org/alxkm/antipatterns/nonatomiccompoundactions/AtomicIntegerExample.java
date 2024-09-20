package org.alxkm.antipatterns.nonatomiccompoundactions;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * Another approach is to use the atomic classes provided by the java.util.concurrent package,
 * such as AtomicInteger, which provides atomic operations for integers.
 * <p>
 * <p>
 * In this version, the incrementIfLessThan method uses AtomicInteger and its compareAndSet method to ensure
 * that the compound action is performed atomically.
 * <p>
 * This approach leverages the atomic classes in the java.util.concurrent package to provide thread safety without explicit synchronization.
 *
 */
public class AtomicIntegerExample {
    private final AtomicInteger counter = new AtomicInteger();

    /**
     * Increments the counter if it is less than a specified limit.
     * This method uses atomic operations to ensure thread safety.
     *
     * @param limit the limit to check against.
     */
    public void incrementIfLessThan(int limit) {
        while (true) {
            int current = counter.get();
            if (current >= limit) {
                break;
            }
            if (counter.compareAndSet(current, current + 1)) {
                break;
            }
        }
    }

    /**
     * Returns the counter value.
     *
     * @return the counter value.
     */
    public int getCounter() {
        return counter.get();
    }

    public static void main(String[] args) {
        AtomicIntegerExample example = new AtomicIntegerExample();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                example.incrementIfLessThan(10);
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

        System.out.println("Final counter value: " + example.getCounter());
    }
}

