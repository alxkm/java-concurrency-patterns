package org.alxkm.antipatterns.threadsinsteadoftasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the "threads instead of tasks" antipattern and its two fixes.
 * <p>
 * The antipattern is not that threads are bad, it is that the thread count tracks the workload. Each
 * example reports how many distinct threads ran its tasks, so the difference is measured here rather
 * than described.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class ThreadsInsteadOfTasksTest {

    @Test
    void directThreadsUseOnePerTask() throws InterruptedException {
        assertEquals(4, new DirectThreadManagement().performTask(4));
        assertEquals(20, new DirectThreadManagement().performTask(20),
                "creating a thread per task means the count grows with the work, without a bound");
    }

    @Test
    void executorReusesAFixedPoolHoweverMuchWorkArrives() throws InterruptedException {
        int small = new ExecutorFrameworkExample().performTask(4);
        int large = new ExecutorFrameworkExample().performTask(20);

        assertTrue(small <= ExecutorFrameworkExample.POOL_SIZE,
                "4 tasks used " + small + " threads, expected at most " + ExecutorFrameworkExample.POOL_SIZE);
        assertTrue(large <= ExecutorFrameworkExample.POOL_SIZE,
                "20 tasks used " + large + " threads, expected at most " + ExecutorFrameworkExample.POOL_SIZE);
    }

    /**
     * The contrast, in one assertion: five times the work costs five times the threads without a pool
     * and nothing at all with one.
     */
    @Test
    void onlyTheUnpooledThreadCountGrowsWithTheWorkload() throws InterruptedException {
        assertEquals(20, new DirectThreadManagement().performTask(20));
        assertTrue(new ExecutorFrameworkExample().performTask(20) <= ExecutorFrameworkExample.POOL_SIZE);
    }

    @Test
    void completableFutureRunsTheTasks() throws InterruptedException, ExecutionException {
        int threadsUsed = new CompletableFutureExample().performTask();

        assertTrue(threadsUsed >= 1 && threadsUsed <= 2,
                "two tasks on the common pool should use one or two threads, used " + threadsUsed);
    }

    /**
     * Composition is the reason to reach for CompletableFuture rather than a plain executor: the
     * stages are declared up front and each runs when its input is ready, with no thread blocking on
     * another.
     */
    @Test
    void completableFutureComposesStagesInOrder() throws InterruptedException, ExecutionException {
        assertEquals("first -> second -> third", new CompletableFutureExample().composeStages());
    }
}
