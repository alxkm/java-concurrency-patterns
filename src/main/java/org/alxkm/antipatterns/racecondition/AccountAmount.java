package org.alxkm.antipatterns.racecondition;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class representing an account amount with various methods for incrementing the amount value.
 */
public class AccountAmount {
    private int amount; // The current amount value
    private final AtomicInteger atomicInteger = new AtomicInteger(0); // Thread-safe atomic integer
    private final Lock lock = new ReentrantLock(); // Lock for synchronization

    /**
     * Retrieves the current amount value.
     *
     * @return The current amount value.
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Increments the amount value without any synchronization.
     */
    public void unsafeIncrementAmount() {
        amount++;
    }

    /**
     * Increments the amount value with lock-based synchronization.
     */
    public void safeLockIncrementAmount() {
        while (true) {
            if (lock.tryLock()) {
                try {
                    amount++;
                    break;
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Increments the amount value with synchronized block-based synchronization.
     */
    public void safeSynchronizedIncrementAmount() {
        synchronized (AccountAmount.class) {
            amount++;
        }
    }

    /**
     * Increments the amount value using an atomic operation.
     */
    public void incrementAtomicAmount() {
        atomicInteger.incrementAndGet();
    }

    /**
     * Retrieves the atomic integer used for atomic operations.
     *
     * @return The atomic integer.
     */
    public AtomicInteger getAtomicInteger() {
        return atomicInteger;
    }
}
