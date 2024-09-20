package org.alxkm.antipatterns.nonatomiccompoundactions;

/**
 *
 * To resolve this issue, we can synchronize the method to ensure that the compound action is atomic.
 *
 * In this revised example, the incrementIfLessThan method is synchronized,
 * ensuring that the compound action is performed atomically.
 * This prevents race conditions and ensures correct results.
 *
 */
public class AtomicCompoundActionsExample {
    private int counter = 0;

    /**
     * Increments the counter if it is less than a specified limit.
     * This method is synchronized to ensure atomicity.
     *
     * @param limit the limit to check against.
     */
    public synchronized void incrementIfLessThan(int limit) {
        if (counter < limit) { // Check
            counter++; // Then act
        }
    }

    /**
     * Returns the counter value.
     *
     * @return the counter value.
     */
    public synchronized int getCounter() {
        return counter;
    }

    public static void main(String[] args) {
        AtomicCompoundActionsExample example = new AtomicCompoundActionsExample();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                example.incrementIfLessThan(10);
            }
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Final counter value: " + example.getCounter());
    }
}

