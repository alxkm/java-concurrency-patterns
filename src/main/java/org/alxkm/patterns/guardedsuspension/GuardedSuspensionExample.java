package org.alxkm.patterns.guardedsuspension;

/**
 * The GuardedSuspensionExample class demonstrates the Guarded Suspension pattern,
 * where a thread waits for a condition to become true before proceeding with an action.
 */
public class GuardedSuspensionExample {
    private boolean condition = false;

    /**
     * The awaitCondition method waits until the condition becomes true.
     * It synchronizes access to the condition variable and waits using the wait() method.
     */
    public synchronized void awaitCondition() {
        while (!condition) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Proceed with the action
    }

    /**
     * The signalCondition method sets the condition to true and notifies all waiting threads.
     * It synchronizes access to the condition variable and signals using the notifyAll() method.
     */
    public synchronized void signalCondition() {
        condition = true;
        notifyAll();
    }
}

