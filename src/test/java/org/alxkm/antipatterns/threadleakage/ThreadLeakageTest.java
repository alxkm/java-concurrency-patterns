package org.alxkm.antipatterns.threadleakage;

import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ThreadLeakageExample} and {@link ThreadLeakageResolution}.
 * <p>
 * The antipattern is not "threads are bad", it is that thread count grows with the amount of work
 * instead of staying flat. Both classes report how many distinct threads actually ran their tasks, so
 * that growth is the thing measured here rather than something the reader has to take on trust.
 */
class ThreadLeakageTest {

    private static final int POOL_SIZE = 10;

    @Test
    void unpooledWorkCostsOneThreadPerTask() throws InterruptedException {
        int tasks = 50;

        int threadsUsed = new ThreadLeakageExample().startThreads(tasks);

        assertEquals(tasks, threadsUsed,
                "a fresh thread per task means thread count tracks workload, which is the leak");
    }

    @Test
    void pooledWorkReusesAFixedSetOfThreads() throws InterruptedException {
        int tasks = 50;

        int threadsUsed = new ThreadLeakageResolution().startThreads(tasks);

        assertTrue(threadsUsed <= POOL_SIZE,
                "a fixed pool must not exceed its size, but used " + threadsUsed + " threads");
        assertTrue(threadsUsed > 0, "the tasks should have run somewhere");
    }

    /**
     * The same comparison with five times the work. The unpooled count rises with it; the pooled count
     * does not move. That difference is the whole point of the pattern.
     */
    @Test
    void onlyTheUnpooledThreadCountGrowsWithTheWorkload() throws InterruptedException {
        int small = 20;
        int large = 100;

        int unpooledSmall = new ThreadLeakageExample().startThreads(small);
        int unpooledLarge = new ThreadLeakageExample().startThreads(large);
        int pooledSmall = new ThreadLeakageResolution().startThreads(small);
        int pooledLarge = new ThreadLeakageResolution().startThreads(large);

        assertEquals(small, unpooledSmall);
        assertEquals(large, unpooledLarge);
        assertTrue(pooledSmall <= POOL_SIZE && pooledLarge <= POOL_SIZE,
                "pooled runs used " + pooledSmall + " and " + pooledLarge + " threads, both should be <= "
                        + POOL_SIZE);
    }

    /**
     * A pool is single use here: startThreads shuts it down on the way out, so a second call is
     * rejected outright rather than quietly starting threads again.
     * <p>
     * The rejection comes from the default AbortPolicy. Worth knowing, because the alternative
     * policies fail much more quietly: DiscardPolicy drops the task without a word, and
     * CallerRunsPolicy runs it on the calling thread.
     */
    @Test
    void poolRejectsWorkOnceShutDown() throws InterruptedException {
        ThreadLeakageResolution resolution = new ThreadLeakageResolution();
        resolution.startThreads(5);

        assertThrows(RejectedExecutionException.class, () -> resolution.startThreads(5),
                "submitting to a shut-down pool should be rejected, not silently accepted");
    }
}
