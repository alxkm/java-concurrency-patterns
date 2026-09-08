package org.alxkm.patterns.semaphore;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemaphoreExampleTest {
    private static final int THREADS = 10;

    /**
     * Every permit taken must be handed back, so once all callers have finished the semaphore is
     * back at full strength.
     */
    @Test
    void allPermitsAreReturnedAfterUse() throws InterruptedException {
        SemaphoreExample semaphoreExample = new SemaphoreExample();

        runConcurrently(semaphoreExample);

        assertEquals(SemaphoreExample.getPermitCount(), semaphoreExample.getAvailablePermits(),
                "permits were leaked or invented");
    }

    /**
     * The semaphore's actual contract: however many threads pile up outside, no more than the
     * permit count may be inside at once.
     * <p>
     * The peak is recorded from inside the guarded section. The previous version of this test
     * incremented a plain {@code int[]} from ten threads -- an unsynchronized read-modify-write, so
     * the concurrency check was itself a data race -- and it counted threads on their way into
     * accessResource() rather than threads admitted by it, so the number it asserted on had no
     * relationship to the limit being tested.
     */
    @Test
    void neverAdmitsMoreThreadsThanPermits() throws InterruptedException {
        SemaphoreExample semaphoreExample = new SemaphoreExample();

        runConcurrently(semaphoreExample);

        int peak = semaphoreExample.getPeakConcurrentAccesses();
        assertTrue(peak <= SemaphoreExample.getPermitCount(),
                "semaphore admitted " + peak + " threads at once, above its limit of "
                        + SemaphoreExample.getPermitCount());
        assertTrue(peak > 1, "expected the threads to actually overlap, but peak concurrency was " + peak);
        assertEquals(SemaphoreExample.getPermitCount(), semaphoreExample.getAvailablePermits());
    }

    /**
     * Releases all callers at once from a start gate so that they genuinely contend for permits,
     * then waits for the pool to drain.
     */
    private static void runConcurrently(SemaphoreExample semaphoreExample) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        try {
            for (int i = 0; i < THREADS; i++) {
                executor.execute(() -> {
                    try {
                        startGate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    semaphoreExample.accessResource();
                });
            }
            startGate.countDown();
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "threads did not finish in time");
        }
    }
}
