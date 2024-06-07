package org.alxkm.antipatterns.forgottensynchronization;

/***
 *
 * To resolve this issue, we need to synchronize the methods to ensure that only one thread can access these critical sections at a time.
 *
 * */
public class CounterResolution {
    private int count = 0;

    /**
     * Increments the counter by one.
     * This method is synchronized to prevent race conditions.
     */
    public synchronized void increment() {
        count++;
    }

    /**
     * Returns the current value of the counter.
     * This method is synchronized to ensure consistent read operations.
     *
     * @return the current count value.
     */
    public synchronized int getCount() {
        return count;
    }
}
