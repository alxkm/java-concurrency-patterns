package org.alxkm.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link VirtualThreadPinningExample}.
 *
 * Only the unpinned side carries a guarantee worth asserting. A virtual thread that blocks while
 * holding a ReentrantLock releases its carrier, so all of the tasks are in flight at once no matter how
 * many cores the machine has, and total time stays close to one task's blocking time.
 *
 * The pinned side is reported rather than asserted. How much slower it runs depends on the core count,
 * and on Java 24 and later JEP 491 removes the pinning altogether, so a threshold here would be a test
 * of the JDK version rather than of anything in this repository.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class VirtualThreadPinningExampleTest {

    private static final int TASKS = 32;
    private static final long BLOCK_MILLIS = 200;

    @Test
    void unpinnedTasksAllRunConcurrently() throws InterruptedException {
        long elapsed = VirtualThreadPinningExample.runUnpinned(TASKS, BLOCK_MILLIS);

        // Serial execution would take TASKS * BLOCK_MILLIS. A quarter of that is a wide margin and
        // still nowhere near what a carrier-bound run could manage.
        long serialBudget = TASKS * BLOCK_MILLIS;
        assertTrue(elapsed < serialBudget / 4,
                "virtual threads should unmount while blocked, but " + TASKS + " tasks of "
                        + BLOCK_MILLIS + "ms took " + elapsed + "ms");
    }

    @Test
    void bothVariantsCompleteAndTheRatioIsReported() throws InterruptedException {
        VirtualThreadPinningExample.Timings timings =
                VirtualThreadPinningExample.compare(TASKS, BLOCK_MILLIS);

        assertTrue(timings.synchronizedMillis() > 0 && timings.reentrantLockMillis() > 0,
                "both variants should have run");

        System.out.printf("pinning: synchronized=%dms reentrantLock=%dms (%.1fx) on %d cores%n",
                timings.synchronizedMillis(), timings.reentrantLockMillis(),
                (double) timings.synchronizedMillis() / Math.max(1, timings.reentrantLockMillis()),
                Runtime.getRuntime().availableProcessors());
    }
}
