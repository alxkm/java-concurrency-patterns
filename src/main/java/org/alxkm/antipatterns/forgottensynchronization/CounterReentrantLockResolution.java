package org.alxkm.antipatterns.forgottensynchronization;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Instead of using the synchronized keyword, we can use ReentrantLock for more advanced synchronization control.
 *
 * Using ReentrantLock provides more flexibility and control over the synchronization process,
 * including the ability to use tryLock, lockInterruptibly, and other features not available with the synchronized keyword.
 */
public class CounterReentrantLockResolution {
    private int count = 0;
    private final Lock lock = new ReentrantLock();

    /**
     * Increments the counter by one.
     * This method uses a ReentrantLock to prevent race conditions.
     */
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current value of the counter.
     * This method uses a ReentrantLock to ensure consistent read operations.
     *
     * @return the current count value.
     */
    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
