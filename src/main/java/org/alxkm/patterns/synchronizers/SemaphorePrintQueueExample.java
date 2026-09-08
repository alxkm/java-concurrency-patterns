package org.alxkm.patterns.synchronizers;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages access to a print queue, limiting the number of concurrent print jobs.
 */
public class SemaphorePrintQueueExample {
    private static final long PRINT_DURATION_MILLIS = 200;

    private final Semaphore semaphore;
    private final int permits;

    /** How many jobs are printing right now. */
    private final AtomicInteger printing = new AtomicInteger();

    /** The largest value {@link #printing} has ever reached. */
    private final AtomicInteger peakPrinting = new AtomicInteger();

    /**
     * Initializes the print queue with a specific number of permits.
     *
     * @param permits The number of concurrent accesses allowed.
     */
    public SemaphorePrintQueueExample(int permits) {
        this.permits = permits;
        this.semaphore = new Semaphore(permits);
    }

    /**
     * Returns the configured concurrency limit.
     *
     * @return The number of concurrent print jobs allowed.
     */
    public int getPermits() {
        return permits;
    }

    /**
     * Returns the greatest number of jobs that were ever printing simultaneously.
     * <p>
     * Sampling {@link Semaphore#availablePermits()} from outside cannot establish this: it is a
     * snapshot that may be taken between jobs and misses the peak entirely.
     *
     * @return The observed peak concurrency.
     */
    public int getPeakConcurrentJobs() {
        return peakPrinting.get();
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    /**
     * Simulates sending a print job to the print queue.
     * <p>
     * The permit is acquired outside the {@code try} whose {@code finally} releases it. A semaphore
     * does not track who holds what, so releasing a permit that was never acquired hands out a
     * permit that never existed and permanently raises the concurrency limit. Acquiring inside the
     * {@code try} would do precisely that whenever a thread was interrupted while queued.
     *
     * @param jobName The name of the print job.
     */
    public void printJob(String jobName) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            peakPrinting.accumulateAndGet(printing.incrementAndGet(), Math::max);
            System.out.println(Thread.currentThread().getName() + " is printing: " + jobName);
            Thread.sleep(PRINT_DURATION_MILLIS); // Simulate time taken to print
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            printing.decrementAndGet();
            System.out.println(Thread.currentThread().getName() + " has finished printing: " + jobName);
            semaphore.release();
        }
    }

    public static void main(String[] args) {
        SemaphorePrintQueueExample semaphorePrintQueueExample = new SemaphorePrintQueueExample(3); // Allow up to 3 concurrent print jobs

        Runnable printTask = () -> {
            String jobName = "Job-" + Thread.currentThread().threadId();
            semaphorePrintQueueExample.printJob(jobName);
        };

        // Create and start 10 threads to simulate 10 print jobs
        for (int i = 0; i < 10; i++) {
            new Thread(printTask).start();
        }
    }
}
