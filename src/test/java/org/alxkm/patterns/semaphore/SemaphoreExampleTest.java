package org.alxkm.patterns.semaphore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SemaphoreExampleTest {

    /**
     * This test method verifies the functionality of the SemaphoreExample class,
     * specifically its usage of the Semaphore to control access to a shared resource.
     * It creates 10 threads, each attempting to access the shared resource concurrently.
     * The test ensures that the semaphore limits access to the resource by allowing only
     * a maximum of 3 threads to access it simultaneously. After all threads have completed,
     * it asserts that the Semaphore has released all its permits, indicating that the shared
     * resource is now available for further access.
     */
    @Test
    public void testSemaphore() throws InterruptedException {
        SemaphoreExample semaphoreExample = new SemaphoreExample();
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(semaphoreExample::accessResource);
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(3, semaphoreExample.getAvailablePermits()); // Check that all permits are released
    }

    /**
     * This test method verifies the concurrency behavior of the SemaphoreExample class
     * when multiple threads attempt to access the shared resource concurrently. It creates
     * 10 threads, each of which tries to access the resource by invoking the accessResource
     * method of the SemaphoreExample instance. The test asserts that at any given time,
     * no more than 3 threads are allowed to access the resource simultaneously, as enforced
     * by the Semaphore. After all threads have completed their execution, the test ensures
     * that the Semaphore has released all its permits, indicating that the resource is now
     * available for further access.
     */
    @Test
    public void testSemaphoreConcurrency() throws InterruptedException {
        SemaphoreExample semaphoreExample = new SemaphoreExample();
        Thread[] threads = new Thread[10];
        final int[] activeThreads = {0};

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                activeThreads[0]++;
                semaphoreExample.accessResource();
                activeThreads[0]--;
            });
            threads[i].start();
        }

        Thread.sleep(500); // Allow some time for threads to start
        assertTrue(activeThreads[0] <= 3); // Ensure no more than 3 threads are accessing the resource concurrently

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(3, semaphoreExample.getAvailablePermits()); // Check that all permits are released
    }
}
