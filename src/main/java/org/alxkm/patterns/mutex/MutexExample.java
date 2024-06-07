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
     * @return the current value of the counter
     */
    public int getCounter() {
        return counter;
    }
}