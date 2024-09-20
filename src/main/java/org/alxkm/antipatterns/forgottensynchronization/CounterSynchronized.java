package org.alxkm.antipatterns.forgottensynchronization;

/**
 *
 * To resolve this issue, we need to synchronize the methods to ensure that only one thread can access these critical sections at a time.
 *
 */
public class CounterSynchronized {
    private int counter = 0;

    /**
     * Increments the counter by one.
     * This method is synchronized to prevent race conditions.
     */
    public synchronized void increment() {
        counter++;
    }

    /**
     * Returns the current value of the counter.
     * This method is synchronized to ensure consistent read operations.
     *
     * @return the current count value.
     */
    public synchronized int getCounter() {
        return counter;
    }

    public static void main(String[] args) throws InterruptedException {
        CounterSynchronized counter = new CounterSynchronized();

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

        // This will always print 2000 because of proper synchronization
        System.out.println("Final Counter Value (with synchronization): " + counter.getCounter());
    }
}
