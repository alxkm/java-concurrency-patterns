package org.alxkm.patterns.synchronizers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the print queue.
 */
class SemaphorePrintQueueExampleTest {
    private static final int PERMITS = 3;
    private static final int JOBS = 10;

    private SemaphorePrintQueueExample printQueue;

    @BeforeEach
    void setUp() {
        printQueue = new SemaphorePrintQueueExample(PERMITS);
    }

    @Test
    void allJobsComplete() throws InterruptedException {
        submitJobs(JOBS);

        assertEquals(PERMITS, printQueue.getSemaphore().availablePermits(),
                "every job should have returned its permit");
    }

    /**
     * The queue must never print more jobs at once than it has permits.
     * <p>
     * The previous version of this test sampled {@code availablePermits()} from the submitting
     * thread as it queued the jobs, so it recorded the initial permit count and asserted that it
     * equalled the initial permit count -- true regardless of how the queue behaved. It then slept a
     * second after taking the measurement it had already used. Read the high-water mark the queue
     * records from inside the guarded section instead.
     */
    @Test
    void neverPrintsMoreJobsThanPermits() throws InterruptedException {
        submitJobs(JOBS);

        int peak = printQueue.getPeakConcurrentJobs();
        assertTrue(peak <= PERMITS,
                "printed " + peak + " jobs at once, above the limit of " + PERMITS);
        assertTrue(peak > 1, "expected jobs to overlap, but peak concurrency was " + peak);
    }

    /**
     * Submits the given number of jobs, releasing them together so that they contend for permits,
     * and waits for the pool to drain.
     */
    private void submitJobs(int jobs) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(jobs);

        try {
            for (int i = 0; i < jobs; i++) {
                int jobId = i;
                executorService.execute(() -> {
                    try {
                        startGate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    printQueue.printJob("Job-" + jobId);
                });
            }
            startGate.countDown();
        } finally {
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(1, TimeUnit.MINUTES),
                    "print jobs did not finish in time");
        }
    }
}
