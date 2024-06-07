package org.alxkm.patterns.mutex;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MutexExampleTest {
    /**
     * The MutexExample class contains a ReentrantLock instance (lock) used to ensure mutual exclusion.
     * The increment method locks the mutex before incrementing the counter and unlocks it in a finally block to ensure the lock is always released.
     * The getCounter method returns the current value of counter.
     * <p>
     * The test uses an ExecutorService with a fixed thread pool of 10 threads.
     * It submits 1000 tasks to increment the counter.
     * The shutdown method is called on the ExecutorService to stop accepting new tasks, and awaitTermination is used to wait for the completion of the submitted tasks.
     * Finally, the test verifies that the counter value is 1000, indicating that all increments were successful and no increments were lost due to concurrent access.
     */
    @Test
    public void testMutex() throws InterruptedException {
        MutexExample mutexExample = new MutexExample();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Submit 1000 increment tasks to the executor service
        for (int i = 0; i < 1000; i++) {
            executorService.submit(mutexExample::increment);
        }

        // Shutdown the executor service and wait for tasks to finish
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // Verify the counter value
        assertEquals(1000, mutexExample.getCounter());
    }
}
