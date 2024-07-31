package org.alxkm.patterns.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Allows scheduling tasks to be executed at a later point in time or periodically.
 * The Scheduler class provides methods for scheduling tasks to be executed after
 * a specified delay or at regular intervals.
 */
public class Scheduler {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1); // The ScheduledExecutorService for scheduling tasks

    /**
     * Schedules a task to be executed after a specified delay.
     *
     * @param task  The task to be executed.
     * @param delay The delay before the task is executed.
     * @param unit  The time unit of the delay parameter.
     */
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        scheduledExecutorService.schedule(task, delay, unit); // Schedule the task
    }

    /**
     * Shuts down the scheduler, preventing new tasks from being submitted.
     * Any previously submitted tasks will continue to execute.
     */
    public void shutdown() {
        scheduledExecutorService.shutdown(); // Shutdown the scheduler
    }
}

