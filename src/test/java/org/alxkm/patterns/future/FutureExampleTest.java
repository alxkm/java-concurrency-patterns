package org.alxkm.patterns.future;

import org.alxkm.testsupport.Await;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for what get() and cancel() do and do not do. Tasks wait on latches, not sleeps, so each test
 * decides exactly when a task is running, blocked or done. They run on daemon threads, and the executor
 * is shut down with shutdownNow(), so a task a bug leaves running fails the test instead of hanging the
 * build.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class FutureExampleTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "future-test");
        thread.setDaemon(true);
        return thread;
    });

    @AfterEach
    void shutDown() {
        executor.shutdownNow();
    }

    @Test
    void getReturnsTheResult() throws Exception {
        Future<Integer> future = executor.submit(() -> 6 * 7);

        assertEquals(42, FutureExample.getOrCancel(future, 10, TimeUnit.SECONDS));
    }

    /**
     * The trap: get(timeout) gives up, the task does not. It is still running, still holding a pool
     * thread, and will finish with a result nobody reads.
     */
    @Test
    void aTimedOutGetLeavesTheTaskRunning() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<String> future = executor.submit(() -> {
            started.countDown();
            release.await();
            return "late";
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));

        assertThrows(TimeoutException.class, () -> future.get(10, TimeUnit.MILLISECONDS));
        assertFalse(future.isDone(), "the task is still running");

        release.countDown();
        assertEquals("late", future.get(10, TimeUnit.SECONDS));
    }

    @Test
    void getOrCancelInterruptsTheTaskWhenTheWaitTimesOut() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch sawInterrupt = new CountDownLatch(1);
        Future<String> future = executor.submit(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
                return "never";
            } catch (InterruptedException e) {
                sawInterrupt.countDown();
                throw e;
            }
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));

        assertThrows(TimeoutException.class, () -> FutureExample.getOrCancel(future, 10, TimeUnit.MILLISECONDS));

        assertTrue(future.isCancelled());
        assertTrue(sawInterrupt.await(10, TimeUnit.SECONDS), "the task was interrupted");
        assertThrows(CancellationException.class, future::get);
    }

    /**
     * The caller is interrupted while it waits. The result has no reader any more, so the task is
     * cancelled as well, and the caller gets the InterruptedException.
     */
    @Test
    void getOrCancelCancelsTheTaskWhenTheCallerIsInterrupted() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        Future<String> task = executor.submit(() -> {
            release.await();
            return "late";
        });
        AtomicReference<Thread> callerThread = new AtomicReference<>();
        Future<Throwable> caller = executor.submit(() -> {
            callerThread.set(Thread.currentThread());
            try {
                FutureExample.getOrCancel(task, 30, TimeUnit.SECONDS);
                return null;
            } catch (Exception e) {
                return e;
            }
        });
        Await.until("the caller waits in get()", () -> callerThread.get() != null
                && callerThread.get().getState() == Thread.State.TIMED_WAITING);

        callerThread.get().interrupt();

        assertTrue(caller.get(10, TimeUnit.SECONDS) instanceof InterruptedException);
        assertTrue(task.isCancelled());
    }

    /**
     * cancel(false) does not interrupt: the running task goes on to the end. The future was cancelled
     * all the same, so get() throws, and the result the task produced is lost.
     */
    @Test
    void afterCancelTheResultIsLostEvenIfTheTaskFinishes() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        Future<String> future = executor.submit(() -> {
            started.countDown();
            release.await();
            finished.countDown();
            return "computed";
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));

        assertTrue(future.cancel(false));
        release.countDown();

        assertTrue(finished.await(10, TimeUnit.SECONDS), "the task ran to the end");
        assertThrows(CancellationException.class, future::get);
    }

    /**
     * cancel(true) is a request. The interrupt is delivered, but a task that never looks at it keeps
     * running, and keeps its thread, until it is done by its own logic.
     */
    @Test
    void aTaskThatIgnoresTheInterruptKeepsRunningAfterCancel() throws Exception {
        AtomicReference<Thread> taskThread = new AtomicReference<>();
        AtomicBoolean stop = new AtomicBoolean();
        CountDownLatch finished = new CountDownLatch(1);
        Future<?> future = executor.submit(() -> {
            taskThread.set(Thread.currentThread());
            while (!stop.get()) {
                Thread.onSpinWait(); // never checks for an interrupt
            }
            finished.countDown();
        });
        Await.until("the task is running", () -> taskThread.get() != null);

        assertTrue(future.cancel(true));
        Await.until("the interrupt reached the task's thread", () -> taskThread.get().isInterrupted());
        assertEquals(1, finished.getCount(), "cancelled, interrupted, and still running");

        stop.set(true);
        assertTrue(finished.await(10, TimeUnit.SECONDS));
    }

    @Test
    void aTaskThatHasNotStartedNeverRuns() throws Exception {
        ExecutorService single = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "future-test-single");
            thread.setDaemon(true);
            return thread;
        });
        try {
            CountDownLatch release = new CountDownLatch(1);
            single.submit(() -> {
                release.await();
                return null;
            });
            AtomicBoolean ran = new AtomicBoolean();
            Future<?> queued = single.submit(() -> ran.set(true));

            assertTrue(queued.cancel(false));
            release.countDown();
            single.submit(() -> { }).get(10, TimeUnit.SECONDS); // everything queued before it has run

            assertFalse(ran.get());
        } finally {
            single.shutdownNow();
        }
    }

    /**
     * Unlike a ForkJoinTask, a FutureTask hands back the very exception the task threw.
     */
    @Test
    void theCauseOfExecutionExceptionIsWhatTheTaskThrew() {
        IllegalStateException thrownByTask = new IllegalStateException("no such order");
        Future<String> future = executor.submit(() -> {
            throw thrownByTask;
        });

        ExecutionException thrown = assertThrows(ExecutionException.class,
                () -> FutureExample.getOrCancel(future, 10, TimeUnit.SECONDS));
        assertSame(thrownByTask, thrown.getCause());
        assertFalse(future.isCancelled(), "a task that failed was not cancelled");
    }

    @Test
    void mainRuns() throws Exception {
        FutureExample.main(new String[0]);
    }
}
