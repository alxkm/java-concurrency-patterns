package org.alxkm.antipatterns.excessivesynchronization;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Another approach to resolving excessive synchronization is to use atomic variables,
 * which provide thread-safe operations without explicit synchronization.
 * <p>
 * <p>
 * Using AtomicInteger simplifies the code and ensures thread safety without the need for explicit synchronization, providing better performance in highly concurrent scenarios.
 **/
public class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger();

    /**
     * Increments the counter by one.
     * This method uses AtomicInteger for thread-safe increment operation.
     */
    public void increment() {
        count.incrementAndGet();
    }

    /**
     * Decrements the counter by one.
     * This method uses AtomicInteger for thread-safe decrement operation.
     */
    public void decrement() {
        count.decrementAndGet();
    }

    /**
     * Returns the current value of the counter.
     * This method uses AtomicInteger for thread-safe read operation.
     *
     * @return the current count value.
     */
    public int getCount() {
        return count.get();
    }

    /**
     * Main method to demonstrate the usage of AtomicCounter.
     */
    public static void main(String[] args) {
        AtomicCounter counter = new AtomicCounter();

        // Print initial count
        System.out.println("Initial count: " + counter.getCount());

        // Increment the counter
        counter.increment();
        System.out.println("Count after incrementing: " + counter.getCount());

        // Decrement the counter
        counter.decrement();
        System.out.println("Count after decrementing: " + counter.getCount());

        // Increment multiple times
        counter.increment();
        counter.increment();
        counter.increment();
        System.out.println("Count after incrementing three times: " + counter.getCount());

        // Decrement multiple times
        counter.decrement();
        counter.decrement();
        System.out.println("Count after decrementing two times: " + counter.getCount());
    }
}

