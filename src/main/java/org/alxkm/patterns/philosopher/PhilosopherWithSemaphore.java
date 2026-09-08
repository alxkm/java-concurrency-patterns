package org.alxkm.patterns.philosopher;

import java.util.concurrent.Semaphore;

/**
 * The PhilosopherWithSemaphore class represents a philosopher in the dining philosophers problem
 * using semaphores to control access to a limited number of resources.
 */
public class PhilosopherWithSemaphore extends Thread {
    private static final long WORK_MILLIS = 50;

    private final Semaphore sem;

    /**
     * Written by this philosopher's own thread and read by others, so it must be volatile for the
     * write to be visible at all.
     */
    private volatile boolean full = false;

    /**
     * Constructs a PhilosopherWithSemaphore with the specified semaphore and name.
     *
     * @param sem   the semaphore controlling access to the resources
     * @param name  the name of the philosopher
     */
    public PhilosopherWithSemaphore(Semaphore sem, String name) {
        // Thread already carries a name; keeping a second field of the same name would shadow
        // getName() and let the two disagree.
        super(name);
        this.sem = sem;
    }

    /**
     * Returns whether this philosopher has finished its meal.
     *
     * @return {@code true} once the philosopher has eaten.
     */
    public boolean isFull() {
        return full;
    }

    /**
     * The behavior of the philosopher thread.
     * <p>
     * The permit is acquired outside the {@code try} that releases it, so an interruption while
     * queueing cannot release a permit this thread never held.
     */
    @Override
    public void run() {
        if (full) {
            return;
        }
        try {
            sem.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            System.out.println(getName() + " preparing");
            sleep(WORK_MILLIS); // Simulating some work
            full = true;
            System.out.println(getName() + " finished");
        } catch (InterruptedException e) {
            // Being asked to stop is not an error; record it and let the thread wind down.
            Thread.currentThread().interrupt();
        } finally {
            sem.release();
        }
    }
}
