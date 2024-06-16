package org.alxkm.patterns.synchronizers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the PrintQueue class.
 */
public class SemaphorePrintQueueExampleTest {
    private SemaphorePrintQueueExample semaphorePrintQueueExample;

    @BeforeEach
    public void setUp() {
        semaphorePrintQueueExample = new SemaphorePrintQueueExample(3); // Initialize with 3 permits
    }

    @Test
    public void testPrintJob() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Submit 10 print jobs to the print queue
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                String jobName = "Job-" + Thread.currentThread().getId();
                semaphorePrintQueueExample.printJob(jobName);
            });
        }

        executorService.shutdown();
        boolean finished = executorService.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue(finished, "Print jobs did not finish in time");
    }

    @Test
    public void testSemaphoreLimits() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Submit 10 print jobs to the print queue
        int max = 0;
        for (int i = 0; i < 3; i++) {
            executorService.submit(() -> {
                String jobName = "Job-" + Thread.currentThread().getId();
                semaphorePrintQueueExample.printJob(jobName);
            });
            max = Math.max(max, semaphorePrintQueueExample.getSemaphore().availablePermits());
        }

        Thread.sleep(1000); // Short delay to allow some jobs to start
        assertEquals(3, max, "There should be 3 permits in use");

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
}
