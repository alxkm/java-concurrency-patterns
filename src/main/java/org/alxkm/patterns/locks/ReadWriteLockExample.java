package org.alxkm.patterns.locks;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An example class demonstrating the usage of a ReadWriteLock for managing concurrent read and write access to a shared resource.
 */
public class ReadWriteLockExample {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(); // The ReentrantReadWriteLock for synchronization
    private int value; // The shared resource value

    /**
     * Writes a new value to the shared resource.
     *
     * @param newValue The new value to write.
     */
    public void write(int newValue) {
        rwLock.writeLock().lock(); // Acquire the write lock
        try {
            value = newValue; // Write the new value
        } finally {
            rwLock.writeLock().unlock(); // Release the write lock
        }
    }

    /**
     * Reads the current value of the shared resource.
     *
     * @return The current value of the shared resource.
     */
    public int read() {
        rwLock.readLock().lock(); // Acquire the read lock
        try {
            return value; // Read and return the current value
        } finally {
            rwLock.readLock().unlock(); // Release the read lock
        }
    }
}
