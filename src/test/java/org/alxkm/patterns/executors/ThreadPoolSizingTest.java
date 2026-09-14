package org.alxkm.patterns.executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the pools that are configured by hand rather than by factory method.
 * <p>
 * The behaviour worth pinning down is how a ThreadPoolExecutor decides to grow, because the intuitive
 * answer is wrong and the mistake is invisible: the pool simply never uses the capacity you gave it.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class ThreadPoolSizingTest {

    /** Long enough that tasks overlap, short enough to keep the suite quick. */
    private static final long WORK_MILLIS = 100;

    /**
     * A ThreadPoolExecutor grows past its core size only when the QUEUE IS FULL, not when work is
     * waiting. Ten tasks into a queue of ten never fill it, so the pool stays at its core size and
     * maximumPoolSize is never reached, however generous it looks.
     */
    @Test
    void poolStaysAtCoreSizeWhileTheQueueHasRoom() {
        int threadsUsed = ThreadPoolExecutorExample.run(2, 8, 10, 10, WORK_MILLIS);

        assertEquals(2, threadsUsed,
                "the queue absorbed every task, so the pool had no reason to grow past its core size");
    }

    /**
     * Fill the queue and the pool does grow, up to the maximum. This is the same configuration as
     * above with a queue too small to hold the backlog.
     */
    @Test
    void poolGrowsTowardsTheMaximumOnceTheQueueIsFull() {
        // Capacity is maximumPoolSize + queueCapacity = 8, comfortably above the 6 tasks submitted.
        int threadsUsed = ThreadPoolExecutorExample.run(2, 6, 2, 6, WORK_MILLIS);

        assertTrue(threadsUsed > 2,
                "a full queue should have pushed the pool past its core size, used " + threadsUsed);
        assertTrue(threadsUsed <= 6,
                "the pool must never exceed its maximum, used " + threadsUsed);
    }

    /**
     * The third behaviour, and the one that reaches production as an incident. A pool accepts
     * maximumPoolSize + queueCapacity tasks and no more. Past that the RejectedExecutionHandler
     * decides, and the default is AbortPolicy: submit throws.
     *
     * Worth knowing because the alternatives fail far more quietly. DiscardPolicy drops the task
     * without a word, DiscardOldestPolicy throws away the task that has waited longest, and
     * CallerRunsPolicy runs it on the calling thread, which silently turns your web server's
     * acceptor into a worker.
     */
    @Test
    void submittingPastCapacityIsRejected() {
        // core 1, max 2, queue 1: at most 3 tasks can be accepted, and 10 are offered.
        assertThrows(RejectedExecutionException.class,
                () -> ThreadPoolExecutorExample.run(1, 2, 1, 10, WORK_MILLIS),
                "a pool at maximum with a full queue should reject, not silently queue more");
    }

    /**
     * The practical consequence: an unbounded queue makes maximumPoolSize dead configuration, because
     * the queue can never fill. Approximated here with a queue far larger than the workload.
     */
    @Test
    void aQueueThatNeverFillsMakesTheMaximumIrrelevant() {
        int threadsUsed = ThreadPoolExecutorExample.run(1, 50, 10_000, 40, WORK_MILLIS);

        assertEquals(1, threadsUsed,
                "with room to queue everything the pool stays at one thread, whatever the maximum says");
    }

    @Test
    void fixedPoolRunsEveryTaskItAccepts() {
        assertEquals(6, ExecutorServiceExample.run(3, 6, WORK_MILLIS),
                "shutdown() stops new submissions but must not abandon accepted work");
    }
}
