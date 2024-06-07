package org.alxkm.antipatterns.forgottensynchronization;


/***
 *
 * Forgotten synchronization is a common concurrency issue in Java where a method or block of code that should be synchronized is not,
 * potentially leading to inconsistent data states or race conditions. Here's an example demonstrating this problem along with a resolution.
 *
 *
 * In this example, the increment method and the getCount method are not synchronized,
 * leading to potential race conditions when accessed by multiple threads simultaneously.
 *
 * */
public class CounterExample {
    private int count = 0;

    /**
     * Increments the counter by one.
     * This method is not synchronized, leading to potential race conditions.
     */
    public void increment() {
        count++;
    }

    /**
     * Returns the current value of the counter.
     * This method is not synchronized, which may result in reading an inconsistent value.
     *
     * @return the current count value.
     */
    public int getCount() {
        return count;
    }
}
