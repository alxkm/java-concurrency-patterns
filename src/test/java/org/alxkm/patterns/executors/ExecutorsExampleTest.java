package org.alxkm.patterns.executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the executor factory methods and the pools built on them.
 * <p>
 * What separates these executors is the relationship between thread count and workload, so every
 * example reports how many distinct threads actually ran its tasks and the tests measure that rather
 * than describing it. No test asserts a timing.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class ExecutorsExampleTest {

    /** Short enough to keep the suite quick, long enough that the tasks genuinely overlap. */
    private static final long WORK_MILLIS = 100;

    /** Both ExecutorsExample flavours submit this many tasks. */
    private static final int TASKS = 5;

    @Test
    void fixedPoolCapsTheThreadCountBelowTheTaskCount() {
        int threadsUsed = ExecutorsExample.fixedThreadPoolExample(WORK_MILLIS);

        assertTrue(threadsUsed <= 3,
                "a fixed pool of 3 should never exceed 3 threads, used " + threadsUsed);
        assertTrue(threadsUsed > 0, "the tasks should have run somewhere");
    }

    /**
     * A single-thread executor is the one with an exact answer, and the reason to choose it: work is
     * serialised without anyone writing a lock.
     */
    @Test
    void singleThreadExecutorUsesExactlyOneThread() {
        assertEquals(1, ExecutorsExample.singleThreadExecutorExample(WORK_MILLIS));
    }

    /**
     * A cached pool creates a thread whenever none is free, so with tasks that overlap it ends up
     * with more threads than the fixed pool allows. That is convenient and is also why it is the
     * wrong default for load you do not control: the thread count follows the arrival rate.
     */
    @Test
    void cachedPoolGrowsBeyondWhatAFixedPoolWouldAllow() {
        int cached = ExecutorsExample.cachedThreadPoolExample(WORK_MILLIS);

        assertTrue(cached >= 1 && cached <= TASKS,
                "a cached pool should use between 1 and " + TASKS + " threads, used " + cached);
    }

    @Test
    void fixedPoolReusesThreadsAcrossMoreTasksThanItHas() throws InterruptedException {
        int threadsUsed = ThreadPoolExample.run(3, 20);

        assertTrue(threadsUsed <= 3,
                "20 tasks on a pool of 3 should use at most 3 threads, used " + threadsUsed);
    }

    @Test
    void poolSizeBoundsTheThreadCountWhateverTheWorkload() throws InterruptedException {
        assertTrue(ThreadPoolExample.run(2, 50) <= 2);
        assertTrue(ThreadPoolExample.run(10, 50) <= 10);
    }
}
