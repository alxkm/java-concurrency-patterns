package org.alxkm.antipatterns.nonatomiccompoundactions;

/**
 *
 * Non-atomic compound actions occur when compound actions (e.g., check-then-act, read-modify-write) are performed without proper synchronization,
 * leading to race conditions and incorrect results. Here's an example demonstrating this problem along with a resolution.
 * <p>
 * In this example, the incrementIfLessThan method performs a non-atomic compound action.
 * If multiple threads execute this method concurrently, they may both pass the check before either increments the counter,
 * leading to incorrect results.
 *
 */
public class NonAtomicCompoundActionsExample {
    private int counter = 0;

    /**
     * Increments the counter if it is less than a specified limit.
     * This method is not synchronized, leading to potential race conditions.
     *
     * @param limit the limit to check against.
     */
    public void incrementIfLessThan(int limit) {
        if (counter < limit) { // Check
            counter++; // Then act
        }
    }

    /**
     * Returns the counter value.
     *
     * @return the counter value.
     */
    public int getCounter() {
        return counter;
    }

    public static void main(String[] args) {
        NonAtomicCompoundActionsExample example = new NonAtomicCompoundActionsExample();

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
