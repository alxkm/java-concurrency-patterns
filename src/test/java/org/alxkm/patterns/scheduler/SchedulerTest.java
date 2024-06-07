package org.alxkm.patterns.scheduler;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulerTest {

    /**
     * This test method verifies the functionality of the Scheduler class for scheduling
     * tasks to run after a certain delay. It creates a Scheduler instance and a CountDownLatch
     * with an initial count of 1. The scheduler is then used to schedule a task that counts
     * down the latch after a delay of 1 second. The test waits for the latch to count down
     * within 2 seconds, ensuring that the scheduled task is executed within the expected time
     * frame. Finally, the scheduler is shut down to release any associated resources.
     */
    @Test
    public void testScheduler() throws InterruptedException {
        Scheduler scheduler = new Scheduler();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(latch::countDown, 1, TimeUnit.SECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        scheduler.shutdown();
    }
}
