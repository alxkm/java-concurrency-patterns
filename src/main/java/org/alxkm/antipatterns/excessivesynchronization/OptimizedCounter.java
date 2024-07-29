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

    public static void main(String[] args) {
        OptimizedCounter counter = new OptimizedCounter();

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

