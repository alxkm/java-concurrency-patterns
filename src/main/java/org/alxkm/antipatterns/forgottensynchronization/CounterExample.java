package org.alxkm.antipatterns.forgottensynchronization;

/**
 *
 * Forgotten synchronization is a common concurrency issue in Java where a method or block of code that should be synchronized is not,
 * potentially leading to inconsistent data states or race conditions. Here's an example demonstrating this problem along with a resolution.
 *
 *
 * In this example, the increment method and the getCount method are not synchronized,
 * leading to potential race conditions when accessed by multiple threads simultaneously.
 *
 */
public class CounterExample {
    private int counter = 0;

    /**
     * Increments the counter by one.
     * This method is not synchronized, leading to potential race conditions.
     */
    public void increment() {
        counter++;
    }

    /**
     * Returns the current value of the counter.
     * This method is not synchronized, which may result in reading an inconsistent value.
     *
     * @return the current count value.
     */
    public int getCounter() {
        return counter;
    }


    public static void main(String[] args) throws InterruptedException {
        CounterExample counter = new CounterExample();

        // Create two threads that both call increment 1000 times
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        });

        t1.start();
        t2.start();

        // Wait for threads to finish
        t1.join();
        t2.join();

        // This will likely print a number less than 2000 due to race conditions
        System.out.println("Final Counter Value (without synchronization): " + counter.getCounter());
    }
}
