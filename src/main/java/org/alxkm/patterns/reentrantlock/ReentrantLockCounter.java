package org.alxkm.patterns.reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple counter implementation using a ReentrantLock for synchronization.
 * The ReentrantLockCounter class provides methods for incrementing and
 * retrieving a counter value with exclusive locking.
 */
public class ReentrantLockCounter {
    private final ReentrantLock lock = new ReentrantLock(); // The ReentrantLock for synchronization
    private int counter; // The shared counter value

    /**
     * Increments the counter value with exclusive locking.
     */
    public void incrementCounter() {
        lock.lock(); // Acquire the lock
        try {
            counter++; // Increment the counter
        } finally {
            lock.unlock(); // Release the lock
        }
    }

    /**
     * Retrieves the current value of the counter.
     *
     * @return The current value of the counter.
     */
    public int getCounter() {
        return counter; // Return the current value of the counter
    }
}
