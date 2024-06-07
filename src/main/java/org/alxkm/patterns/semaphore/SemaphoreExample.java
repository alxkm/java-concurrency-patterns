package org.alxkm.patterns.semaphore;

import java.util.concurrent.Semaphore;

/**
 * Limits the number of threads that can access a resource.
 * The SemaphoreExample class demonstrates the usage of a Semaphore to control access to a shared resource
 * by limiting the number of concurrent threads that can access it.
 */
public class SemaphoreExample {
    private final Semaphore semaphore = new Semaphore(3); // The Semaphore with an initial permit count of 3

    /**
     * Accesses the shared resource, acquiring a permit from the semaphore.
     * If no permits are available, the method blocks until a permit becomes available.
     * Once access is obtained, the method simulates resource access by sleeping for a short duration.
     */
    public void accessResource() {
        try {
            semaphore.acquire(); // Acquire a permit
            // Simulate resource access
            Thread.sleep(100); // Simulate resource access for 100 milliseconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release(); // Release the permit
        }
    }

    /**
     * Retrieves the number of permits currently available in the semaphore.
     *
     * @return The number of available permits.
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits(); // Return the number of available permits
    }
}

