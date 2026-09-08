package org.alxkm.patterns.semaphore;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits the number of threads that can access a resource.
 * The SemaphoreExample class demonstrates the usage of a Semaphore to control access to a shared resource
 * by limiting the number of concurrent threads that can access it.
 */
public class SemaphoreExample {
    private static final int PERMITS = 3;

    private final Semaphore semaphore = new Semaphore(PERMITS); // The Semaphore with an initial permit count of 3

    /** How many threads are inside the guarded section right now. */
    private final AtomicInteger inFlight = new AtomicInteger();

    /** The largest value {@link #inFlight} has ever reached. */
    private final AtomicInteger peakInFlight = new AtomicInteger();

    /**
     * Accesses the shared resource, acquiring a permit from the semaphore.
     * If no permits are available, the method blocks until a permit becomes available.
     * Once access is obtained, the method simulates resource access by sleeping for a short duration.
     * <p>
     * Note where the {@code try} begins. Acquiring outside it is what makes the {@code finally}
     * correct: a semaphore has no notion of ownership, so releasing a permit the thread never
     * acquired simply invents one and raises the limit for everybody. Had {@code acquire()} sat
     * inside the {@code try}, an interruption while blocked -- when no permit was held -- would
     * still run the {@code finally} and do exactly that.
     */
    public void accessResource() {
        try {
            semaphore.acquire(); // Acquire a permit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            // Record the high-water mark from inside the guarded section. Counting on the way to
            // acquire() instead would measure threads queueing, not threads admitted, which says
            // nothing about whether the semaphore is holding the line.
            peakInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            // Simulate resource access
            Thread.sleep(100); // Simulate resource access for 100 milliseconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            inFlight.decrementAndGet();
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

    /**
     * Returns the number of permits the semaphore was created with.
     *
     * @return The permit count, and therefore the concurrency limit.
     */
    public static int getPermitCount() {
        return PERMITS;
    }

    /**
     * Returns the greatest number of threads that were ever inside the guarded section at once.
     * <p>
     * This is the semaphore's contract made observable: it must never exceed
     * {@link #getPermitCount()}, no matter how many threads pile up outside.
     *
     * @return The observed peak concurrency.
     */
    public int getPeakConcurrentAccesses() {
        return peakInFlight.get();
    }
}

