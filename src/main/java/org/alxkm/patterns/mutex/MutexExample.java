package org.alxkm.patterns.mutex;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A mutex (short for mutual exclusion) is a synchronization primitive used to protect shared resources
 * from concurrent access by multiple threads.
 * In Java, ReentrantLock from the java.util.concurrent.locks package is commonly used as a mutex.
 */
public class MutexExample {
    private final Lock lock = new ReentrantLock();
    private int counter = 0;

    /**
     * Increments the counter in a thread-safe manner.
     */
    public void increment() {
        lock.lock();
        try {
            counter++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves the current value of the counter.
     *
     * Read under the same lock that guards the writes. Mutual exclusion on its own is not enough:
     * without the lock here there is no happens-before edge between an increment and this read, so a
     * caller could observe a stale value no matter how carefully the increment was guarded. Every
     * other counter in this repository guards its getter the same way.
     *
     * @return the current value of the counter
     */
    public int getCounter() {
        lock.lock();
        try {
            return counter;
        } finally {
            lock.unlock();
        }
    }
}
