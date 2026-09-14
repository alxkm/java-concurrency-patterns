package org.alxkm.patterns.executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the hand-written {@link AbstractExecutorServiceExample}.
 * <p>
 * A custom ExecutorService is mostly a set of promises about its own lifecycle, and those are exactly
 * what an implementation gets wrong quietly. Two of them were wrong here until these tests were
 * written: the running-task counter only ever incremented, so {@code isTerminated} could never become
 * true and {@code awaitTermination} always burned its full timeout before returning false; and
 * {@code shutdownNow} returned a collection nothing ever added to.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class AbstractExecutorServiceExampleTest {

    @Test
    void submittedTasksRunAndDeliverTheirResults() throws Exception {
        AbstractExecutorServiceExample executor = new AbstractExecutorServiceExample();

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            futures.add(executor.submit(() -> taskId * 2));
        }

        for (int i = 0; i < futures.size(); i++) {
            assertEquals(i * 2, futures.get(i).get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();
    }

    /**
     * The promise that was broken. Once every task has finished and the executor is shut down, it is
     * terminated, and awaitTermination says so promptly instead of running out its timeout.
     */
    @Test
    void terminatesOnceItsTasksHaveFinished() throws Exception {
        AbstractExecutorServiceExample executor = new AbstractExecutorServiceExample();
        Future<String> task = executor.submit(() -> "done");
        assertEquals("done", task.get(10, TimeUnit.SECONDS));

        executor.shutdown();

        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "every task has finished, so the executor should report termination");
        assertTrue(executor.isTerminated());
    }

    @Test
    void isNotTerminatedWhileATaskIsStillRunning() throws Exception {
        AbstractExecutorServiceExample executor = new AbstractExecutorServiceExample();
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);

        Future<?> task = executor.submit(() -> {
            started.countDown();
            release.await();
            return null;
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));

        executor.shutdown();
        assertTrue(executor.isShutdown(), "shutdown() should stop it accepting work immediately");
        assertFalse(executor.isTerminated(), "a task is still running, so it is not terminated");
        assertFalse(executor.awaitTermination(200, TimeUnit.MILLISECONDS),
                "awaitTermination should time out while work is outstanding");

        release.countDown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    void rejectsWorkOnceShutDown() {
        AbstractExecutorServiceExample executor = new AbstractExecutorServiceExample();
        executor.shutdown();

        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> "too late"));
        assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> { }));
    }

    /**
     * shutdownNow must actually stop what is running, not merely stop accepting more.
     * <p>
     * Its empty return value is correct rather than a stub: this executor starts every task at once on
     * its own thread, so nothing is ever queued and there is nothing awaiting execution to hand back.
     */
    @Test
    void shutdownNowInterruptsRunningTasksAndQueuesNothing() throws Exception {
        AbstractExecutorServiceExample executor = new AbstractExecutorServiceExample();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);

        executor.execute(() -> {
            started.countDown();
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                interrupted.countDown();
            }
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));

        List<Runnable> pending = executor.shutdownNow();

        assertTrue(interrupted.await(10, TimeUnit.SECONDS),
                "shutdownNow should interrupt the running task");
        assertTrue(pending.isEmpty(), "nothing is ever queued, so there is nothing to return");
        assertTrue(executor.isShutdown());
    }

    /**
     * A failing task must not cost the caller the results of the others: the exception arrives through
     * that task's own Future.
     */
    @Test
    void oneFailingTaskDoesNotAffectTheRest() throws Exception {
        AbstractExecutorServiceExample executor = new AbstractExecutorServiceExample();

        Future<Integer> ok = executor.submit(() -> 1);
        Future<Integer> boom = executor.submit(() -> {
            throw new IllegalStateException("task failed on purpose");
        });

        assertEquals(1, ok.get(10, TimeUnit.SECONDS));
        ExecutionException thrown = assertThrows(ExecutionException.class, () -> boom.get(10, TimeUnit.SECONDS));
        assertTrue(thrown.getCause() instanceof IllegalStateException);

        executor.shutdown();
    }
}
