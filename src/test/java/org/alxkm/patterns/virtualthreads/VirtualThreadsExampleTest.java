package org.alxkm.patterns.virtualthreads;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link VirtualThreadsExample}.
 * <p>
 * The examples take a size argument so the demo can run at its full scale from main while the tests use
 * something small. Nothing here asserts a timing: the performance comparison reports both numbers and
 * the test checks the work completed, because a shared CI runner can easily invert a ratio.
 */
class VirtualThreadsExampleTest {

    @Test
    void basicExampleRunsOnAVirtualThread() {
        assertTrue(VirtualThreadsExample.basicVirtualThreadExample(),
                "Thread.ofVirtual() should produce a thread that reports isVirtual()");
    }

    /**
     * Both kinds of thread must finish the same work. The interesting difference is how long they take,
     * which is printed rather than asserted.
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void bothThreadKindsCompleteTheSameBlockingWorkload() {
        VirtualThreadsExample.Timings timings = VirtualThreadsExample.performanceComparison(500, 20);

        assertTrue(timings.platformMillis() >= 0 && timings.virtualMillis() >= 0,
                "both runs should have completed");

        System.out.printf("blocking workload: platform=%dms virtual=%dms%n",
                timings.platformMillis(), timings.virtualMillis());
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void producerConsumerDeliversEveryItem() {
        int items = 200;

        assertEquals(items, VirtualThreadsExample.producerConsumerWithVirtualThreads(items),
                "every produced item should be consumed exactly once");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void threadPerTaskExecutorRunsEveryTask() {
        // 50 tasks, each summing 0..999 (499500) and adding its own index (0..49 sums to 1225).
        int expected = 50 * 499_500 + 1_225;

        assertEquals(expected, VirtualThreadsExample.virtualThreadPoolExample(),
                "every task should have contributed its result");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void completableFutureChainRunsAllThreeStagesInOrder() {
        String result = VirtualThreadsExample.virtualThreadsWithCompletableFuture();

        assertEquals("Step 1 completed -> Step 2 completed -> Step 3 completed", result,
                "the stages should compose in order on the virtual thread executor");
    }

    /**
     * The headline claim of virtual threads: a task count that would exhaust platform threads runs
     * without special handling. Scaled down here; main runs it at 100,000.
     */
    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void massiveConcurrencyCompletesEveryTask() {
        // Kept well below the 100,000 the demo uses: under JaCoCo instrumentation every task costs
        // more, and the point being checked is that all of them complete, not how many there are.
        int tasks = 10_000;

        assertEquals(tasks, VirtualThreadsExample.massiveConcurrencyExample(tasks),
                "every one of the " + tasks + " virtual threads should have completed");
    }
}
