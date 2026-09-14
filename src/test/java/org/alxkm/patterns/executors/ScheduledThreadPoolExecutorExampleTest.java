package org.alxkm.patterns.executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ScheduledThreadPoolExecutorExample}.
 * <p>
 * Scheduling is the one area where a test has to touch the clock, so the assertions are deliberately
 * coarse: that a one-off task runs exactly once, that the repeating ones run more than once, and that
 * nothing runs before its initial delay. Asserting an exact number of repetitions would be asserting
 * the scheduler's punctuality on a loaded machine, which is not a property of this code.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class ScheduledThreadPoolExecutorExampleTest {

    /** Small enough to keep the suite quick; the demo in main uses a second. */
    private static final long UNIT_MILLIS = 100;

    @Test
    void oneShotTaskRunsExactlyOnce() throws InterruptedException {
        // Runs for long enough that a repeating task would have fired several times.
        ScheduledThreadPoolExecutorExample.Runs runs =
                ScheduledThreadPoolExecutorExample.run(UNIT_MILLIS, 12 * UNIT_MILLIS);

        assertEquals(1, runs.oneShot(), "schedule() fires once and is then done");
    }

    @Test
    void bothRepeatingFormsKeepFiring() throws InterruptedException {
        ScheduledThreadPoolExecutorExample.Runs runs =
                ScheduledThreadPoolExecutorExample.run(UNIT_MILLIS, 15 * UNIT_MILLIS);

        assertTrue(runs.atFixedRate() > 1,
                "scheduleAtFixedRate should have repeated, ran " + runs.atFixedRate() + " times");
        assertTrue(runs.withFixedDelay() > 1,
                "scheduleWithFixedDelay should have repeated, ran " + runs.withFixedDelay() + " times");
    }

    /**
     * Nothing fires before its initial delay. Stopping the executor inside the delay window is the
     * cleanest way to check that, and it needs no timing tolerance.
     */
    @Test
    void nothingRunsBeforeItsInitialDelay() throws InterruptedException {
        // Every task has an initial delay of at least one unit; stop after half of one.
        ScheduledThreadPoolExecutorExample.Runs runs =
                ScheduledThreadPoolExecutorExample.run(10 * UNIT_MILLIS, 5 * UNIT_MILLIS);

        assertEquals(0, runs.oneShot());
        assertEquals(0, runs.atFixedRate());
        assertEquals(0, runs.withFixedDelay());
    }

    /**
     * shutdownNow has to cancel the repeating tasks. shutdown() alone lets an already scheduled
     * periodic task keep firing, which is a common way for a "stopped" scheduler to keep working.
     */
    @Test
    void schedulerStopsWhenToldTo() throws InterruptedException {
        ScheduledThreadPoolExecutorExample.Runs first =
                ScheduledThreadPoolExecutorExample.run(UNIT_MILLIS, 12 * UNIT_MILLIS);
        Thread.sleep(5 * UNIT_MILLIS);
        ScheduledThreadPoolExecutorExample.Runs second =
                ScheduledThreadPoolExecutorExample.run(UNIT_MILLIS, 12 * UNIT_MILLIS);

        // Counters are per run, so a scheduler that kept firing after shutdown would not show up
        // here as a larger number; it would show up as the run never terminating. Reaching this
        // point at all is the assertion.
        assertTrue(first.atFixedRate() > 0 && second.atFixedRate() > 0);
    }
}
