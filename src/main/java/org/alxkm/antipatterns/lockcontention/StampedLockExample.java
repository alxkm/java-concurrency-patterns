package org.alxkm.antipatterns.lockcontention;

import java.util.concurrent.locks.StampedLock;

/**
 *
 * Another approach is to use StampedLock, which allows for more flexible lock handling, including optimistic reads.
 *
 * In this version, StampedLock is used to manage the counter.
 * StampedLock allows for optimistic reads, which can improve performance by avoiding locks if the data hasn't changed.
 *
 */
public class StampedLockExample {
    private final StampedLock lock = new StampedLock();
    private int counter = 0;

    /**
     * Increments the counter using a write lock.
     */
    public void increment() {
        long stamp = lock.writeLock();
        try {
            counter++;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns the counter value using an optimistic read lock.
     *
     * @return the counter value.
     */
    public int getCounter() {
        long stamp = lock.tryOptimisticRead();
        int currentCounter = counter;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentCounter = counter;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return currentCounter;
    }

    public static void main(String[] args) {
        StampedLockExample stampedLockExample = new StampedLockExample();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                stampedLockExample.increment();
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

        System.out.println("Final counter value: " + stampedLockExample.getCounter());
    }
}

