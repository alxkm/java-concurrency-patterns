package org.alxkm.antipatterns.excessivesynchronization;

/**
 * To resolve this issue, we can minimize the scope of synchronization to only the critical sections of the code, thus reducing the potential for contention.
 * <p>
 * By synchronizing only the critical sections of the methods, we reduce the contention and improve the overall performance of the class when accessed by multiple threads.
 */
public class OptimizedCounter {
    private int count = 0;

    /**
     * Increments the counter by one.
     * Synchronization is applied only to the critical section.
     */
    public void increment() {
        synchronized (this) {
            count++;
        }
    }

    /**
     * Decrements the counter by one.
     * Synchronization is applied only to the critical section.
     */
    public void decrement() {
        synchronized (this) {
            count--;
        }
    }

    /**
     * Returns the current value of the counter.
     * Synchronization is applied only to the critical section.
     *
     * @return the current count value.
     */
    public int getCount() {
        synchronized (this) {
            return count;
        }
    }
}

