package org.alxkm.patterns.philosopher;

import java.util.concurrent.Semaphore;

/**
 * The PhilosopherWithSemaphore class represents a philosopher in the dining philosophers problem
 * using semaphores to control access to a limited number of resources.
 */
public class PhilosopherWithSemaphore extends Thread {
    private final Semaphore sem;
    private boolean full = false;
    private final String name;

    /**
     * Constructs a PhilosopherWithSemaphore with the specified semaphore and name.
     *
     * @param sem   the semaphore controlling access to the resources
     * @param name  the name of the philosopher
     */
    public PhilosopherWithSemaphore(Semaphore sem, String name) {
        this.sem = sem;
        this.name = name;
    }

    /**
     * The behavior of the philosopher thread.
     */
    public void run() {
        try {
            if (!full) {
                sem.acquire();
                System.out.println(name + " preparing");
                sleep(300); // Simulating some work
                full = true;
                System.out.println(name + " finished");
                sem.release();
                sleep(300); // Simulating some work
            }
        } catch (InterruptedException e) {
            System.out.println("An error occurred");
        }
    }
}
