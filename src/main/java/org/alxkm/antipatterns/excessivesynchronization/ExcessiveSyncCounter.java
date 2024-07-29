package org.alxkm.antipatterns.excessivesynchronization;

/**
 * Excessive synchronization occurs when synchronization is used more broadly than necessary,
 * which can lead to reduced performance due to unnecessary blocking of threads. This can also increase the risk of deadlocks.
 * <p>
 * In this example, each method is synchronized on the entire method,
 * potentially leading to performance bottlenecks if these methods are called frequently from different threads.
 */
public class ExcessiveSyncCounter {
    private int count = 0;

    /**
     * Increments the counter by one.
     * This method synchronizes the entire method, which might not be necessary.
     */
    public synchronized void increment() {
        count++;
    }

    /**
     * Decrements the counter by one.
     * This method synchronizes the entire method, which might not be necessary.
     */
    public synchronized void decrement() {
        count--;
    }

    /**
     * Returns the current value of the counter.
     * This method synchronizes the entire method, which might not be necessary.
     *
     * @return the current count value.
     */
    public synchronized int getCount() {
        return count;
    }

    public static void main(String[] args) {
        ExcessiveSyncCounter counter = new ExcessiveSyncCounter();

        // Create threads to perform operations on the counter
        Thread incrementThread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        });

        Thread incrementThread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        });

        Thread decrementThread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.decrement();
            }
        });

        Thread decrementThread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.decrement();
            }
        });

        // Start the threads
        incrementThread1.start();
        incrementThread2.start();
        decrementThread1.start();
        decrementThread2.start();

        // Wait for all threads to complete
        try {
            incrementThread1.join();
            incrementThread2.join();
            decrementThread1.join();
            decrementThread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Print the final count
        System.out.println("Final count: " + counter.getCount());
    }
}
