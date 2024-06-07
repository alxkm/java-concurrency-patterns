package org.alxkm.patterns.locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Separates read and write access to a shared resource to improve concurrency.
 * The ReentrantReadWriteLockCounter class provides methods for incrementing and
 * retrieving a counter value with separate locks for reading and writing.
 */
public class ReentrantReadWriteLockCounter {
    private int counter; // The shared counter value
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(); // The ReentrantReadWriteLock for synchronization
    private final Lock readLock = rwLock.readLock(); // The read lock
    private final Lock writeLock = rwLock.writeLock(); // The write lock

    /**
     * Increments the counter value with exclusive write access.
     */
    public void incrementCounter() {
        writeLock.lock(); // Acquire the write lock
        try {
            counter += 1; // Increment the counter
        } finally {
            writeLock.unlock(); // Release the write lock
        }
    }

    /**
     * Retrieves the current value of the counter with shared read access.
     *
     * @return The current value of the counter.
     */
    public int getCounter() {
        readLock.lock(); // Acquire the read lock
        try {
            return counter; // Return the current value of the counter
        } finally {
            readLock.unlock(); // Release the read lock
        }
    }
}
