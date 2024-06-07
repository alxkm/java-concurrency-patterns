package org.alxkm.patterns.synchronizers;

import java.util.concurrent.Semaphore;

/**
 * Manages access to a print queue, limiting the number of concurrent print jobs.
 */
public class SemaphorePrintQueueExample {
    private final Semaphore semaphore;

    /**
     * Initializes the print queue with a specific number of permits.
     *
     * @param permits The number of concurrent accesses allowed.
     */
    public SemaphorePrintQueueExample(int permits) {
        semaphore = new Semaphore(permits);
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    /**
     * Simulates sending a print job to the print queue.
     *
     * @param jobName The name of the print job.
     */
    public void printJob(String jobName) {
        try {
            semaphore.acquire();
            System.out.println(Thread.currentThread().getName() + " is printing: " + jobName);
            Thread.sleep(2000); // Simulate time taken to print
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println(Thread.currentThread().getName() + " has finished printing: " + jobName);
            semaphore.release();
        }
    }

    public static void main(String[] args) {
        SemaphorePrintQueueExample semaphorePrintQueueExample = new SemaphorePrintQueueExample(3); // Allow up to 3 concurrent print jobs

        Runnable printTask = () -> {
            String jobName = "Job-" + Thread.currentThread().getId();
            semaphorePrintQueueExample.printJob(jobName);
        };

        // Create and start 10 threads to simulate 10 print jobs
        for (int i = 0; i < 10; i++) {
            new Thread(printTask).start();
        }
    }
}
