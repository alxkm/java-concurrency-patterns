package org.alxkm.patterns.synchronizers;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CountDownLatchExampleTest {

    /**
     * The CountDownLatchExample class has a CountDownLatch initialized with a given count.
     * The performTask method simulates task completion by printing a message and decrementing the latch count using countDown().
     * The waitForCompletion method waits until the latch count reaches zero using await(), indicating that all tasks have completed.
     * The getLatch method provides access to the latch for testing purposes.
     * <p>
     * The test initializes CountDownLatchExample with a task count of 5.
     * An ExecutorService with a fixed thread pool is used to submit tasks that perform work and count down the latch.
     * The waitForCompletion method is called to wait for all tasks to complete.
     * The test verifies that the latch count reached zero, ensuring all tasks were completed.
     * The ExecutorService is then shut down and awaits termination.
     */
    @Test
    public void testCountDownLatch() throws InterruptedException {
        int taskCount = 5;
        CountDownLatchExample example = new CountDownLatchExample(taskCount);
        ExecutorService executorService = Executors.newFixedThreadPool(taskCount);

        // Submit tasks to the executor service
        for (int i = 0; i < taskCount; i++) {
            executorService.submit(example::performTask);
        }

        // Wait for all tasks to complete
        example.waitForCompletion();

        // Verify that the latch count reached zero
        assertEquals(0, example.getLatch().getCount());

        // Shutdown the executor service
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
}
