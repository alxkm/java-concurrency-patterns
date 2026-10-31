package org.alxkm.patterns.structured;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;

/**
 * Tests for the scope in {@link StructuredConcurrencyExample}.
 * <p>
 * Structured concurrency makes three promises: the first failure cancels the rest, a deadline bounds the
 * whole group, and no subtask outlives the block. Each has a test below that fails against a scope that
 * breaks it. Where a fast sibling could finish either before or after a failure cancels it, the tests
 * leave it out rather than accept every outcome, since an assertion that accepts every state checks
 * nothing.
 */
public class StructuredConcurrencyExampleTest {

    @Test
    public void testBasicStructuredTaskScope() throws Exception {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            // Fork multiple tasks
            var task1 = scope.fork(() -> "Task 1 result");
            var task2 = scope.fork(() -> "Task 2 result");
            var task3 = scope.fork(() -> "Task 3 result");

            // Wait for completion
            scope.join();
            scope.throwIfFailed();

            // Verify results
            assertEquals("Task 1 result", task1.get());
            assertEquals("Task 2 result", task2.get());
            assertEquals("Task 3 result", task3.get());

            assertEquals("SUCCESS", task1.state());
            assertEquals("SUCCESS", task2.state());
            assertEquals("SUCCESS", task3.state());
        }
    }

    @Test
    public void failureIsRecordedAndRethrownAsItWasThrown() throws InterruptedException {
        RuntimeException failure = new RuntimeException("Task failed");
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            var task = scope.fork(() -> {
                throw failure;
            });

            scope.join();

            assertSame(failure, scope.exception(), "the subtask's own exception, not a wrapper");
            assertSame(failure, assertThrows(RuntimeException.class, scope::throwIfFailed));
            assertEquals("FAILED", task.state());
        }
    }

    @Test
    public void anErrorCountsAsAFailureToo() throws InterruptedException {
        AssertionError failure = new AssertionError("an Error, not an Exception");
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            var task = scope.fork(() -> {
                throw failure;
            });

            scope.join();

            assertSame(failure, scope.exception(), "an Error must not slip past the scope unrecorded");
            assertEquals("FAILED", task.state());
        }
    }

    @Test
    @Timeout(5)
    public void firstFailureCancelsTheSiblings() throws InterruptedException {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            var failingTask = scope.fork(() -> {
                Thread.sleep(10);
                throw new RuntimeException("Quick failure");
            });
            var slowTask = scope.fork(() -> {
                Thread.sleep(10_000);
                return "Slow success";
            });

            scope.join();   // returns once the failure has shut the scope down, not after ten seconds

            assertEquals("Quick failure", scope.exception().getMessage());
            assertEquals("FAILED", failingTask.state());
            assertEquals("CANCELLED", slowTask.state(), "a failure must cancel the work still running");
        }
    }

    @Test
    public void testStructuredTaskScopeWithTimeout() throws InterruptedException {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            var fastTask = scope.fork(() -> {
                Thread.sleep(50);
                return "Fast completed";
            });
            var slowTask = scope.fork(() -> {
                Thread.sleep(5_000);
                return "Slow completed";
            });

            Instant deadline = Instant.now().plus(Duration.ofMillis(300));

            // A timeout is a TimeoutException. An InterruptedException would tell the caller that its
            // own thread was asked to stop, which is a different thing and is handled differently.
            assertThrows(TimeoutException.class, () -> scope.joinUntil(deadline));

            assertEquals("SUCCESS", fastTask.state());
            assertEquals("CANCELLED", slowTask.state());
        }
    }

    /**
     * The deadline bounds the whole join, not each subtask in turn.
     * <p>
     * The first subtask finishes at 200 ms, with 100 ms left before the deadline. A scope that restarts
     * the full 300 ms timeout for the second subtask waits until 500 ms, sees it finish at 450 ms, and
     * returns success 150 ms after the deadline passed.
     */
    @Test
    public void joinUntilKeepsToTheDeadlineAcrossSubtasks() throws InterruptedException {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            scope.fork(() -> {
                Thread.sleep(200);
                return "first";
            });
            var second = scope.fork(() -> {
                Thread.sleep(450);
                return "second";
            });

            Instant deadline = Instant.now().plus(Duration.ofMillis(300));

            assertThrows(TimeoutException.class, () -> scope.joinUntil(deadline),
                    "the second subtask was still running when the deadline passed");
            assertEquals("CANCELLED", second.state());
        }
    }

    /**
     * Cancelled subtasks usually end by throwing, since that is how most code reacts to an interrupt.
     * That is the scope's own doing, not a failure, and must not show up as one.
     * <p>
     * The test joins the subtask's thread itself rather than relying on close(), so that it checks this
     * one property on its own: a close() that returned early would otherwise hide the spurious failure by
     * reading exception() before the subtask got round to recording it.
     */
    @Test
    public void cancellingIsNotRecordedAsAFailure() throws InterruptedException {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            CountDownLatch started = new CountDownLatch(1);
            AtomicReference<Thread> worker = new AtomicReference<>();
            scope.fork(() -> {
                worker.set(Thread.currentThread());
                started.countDown();
                try {
                    Thread.sleep(10_000);
                    return "never";
                } catch (InterruptedException e) {
                    throw new RuntimeException("interrupted", e);   // the usual reaction to a cancel
                }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS), "subtask did not start");

            scope.cancel();
            worker.get().join(5_000);

            assertFalse(worker.get().isAlive(), "the cancelled subtask did not stop");
            assertNull(scope.exception(), "the scope cancelled this subtask; that is not a failure");
        }
    }

    /**
     * close() must not return while a subtask is still running, even one that ignores the interrupt.
     * <p>
     * The subtask spins for 1.2 seconds without checking for interruption. A scope that waits a fixed
     * second and then gives up returns with it still running, outside the block that started it.
     */
    @Test
    @Timeout(10)
    public void closeWaitsForEverySubtaskToFinish() throws InterruptedException {
        var scope = new StructuredConcurrencyExample.StructuredTaskScope();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean finished = new AtomicBoolean();
        scope.fork(() -> {
            started.countDown();
            long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1_200);
            while (System.nanoTime() < end) {
                Thread.onSpinWait();   // deliberately deaf to the interrupt
            }
            finished.set(true);
            return null;
        });
        assertTrue(started.await(5, TimeUnit.SECONDS), "subtask did not start");

        scope.close();

        assertTrue(finished.get(), "close() returned while a subtask was still running");
    }

    @Test
    public void testSubtaskStates() throws Exception {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            var task = scope.fork(() -> {
                Thread.sleep(100);
                return "Completed";
            });

            // Initially should be running
            assertFalse(task.isDone());

            scope.join();
            scope.throwIfFailed();

            // After join should be done
            assertTrue(task.isDone());
            assertEquals("SUCCESS", task.state());
            assertEquals("Completed", task.get());
        }
    }

    @Test
    public void testCancelledTaskScope() {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            var task = scope.fork(() -> {
                Thread.sleep(5000);
                return "Should not complete";
            });

            scope.cancel();

            // Should not be able to fork new tasks
            assertThrows(IllegalStateException.class, () -> {
                scope.fork(() -> "New task");
            });

            // Cancelling a future marks it cancelled at once; there is nothing to wait for.
            assertEquals("CANCELLED", task.state());
        }
    }

    @Test
    public void testParallelComputation() throws Exception {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            // Fork tasks to compute squares
            List<StructuredConcurrencyExample.Subtask<Integer>> tasks = new ArrayList<>();

            for (Integer number : numbers) {
                var task = scope.fork(() -> {
                    // Simulate computation
                    Thread.sleep(10);
                    return number * number;
                });
                tasks.add(task);
            }

            scope.join();
            scope.throwIfFailed();

            // Verify results
            int sum = 0;
            for (int i = 0; i < numbers.size(); i++) {
                int expected = numbers.get(i) * numbers.get(i);
                int actual = tasks.get(i).get();
                assertEquals(expected, actual);
                sum += actual;
            }

            // Sum of squares from 1 to 10 = 385
            assertEquals(385, sum);
        }
    }

    @Test
    public void testNestedStructuredScopes() throws Exception {
        try (var outerScope = new StructuredConcurrencyExample.StructuredTaskScope()) {

            // Fork a task that uses nested structured concurrency
            var outerTask = outerScope.fork(() -> {
                try (var innerScope = new StructuredConcurrencyExample.StructuredTaskScope()) {
                    var innerTask1 = innerScope.fork(() -> "Inner task 1");
                    var innerTask2 = innerScope.fork(() -> "Inner task 2");

                    innerScope.join();
                    innerScope.throwIfFailed();

                    return innerTask1.get() + " + " + innerTask2.get();
                }
            });

            // Fork another parallel task
            var parallelTask = outerScope.fork(() -> "Parallel task");

            outerScope.join();
            outerScope.throwIfFailed();

            assertEquals("Inner task 1 + Inner task 2", outerTask.get());
            assertEquals("Parallel task", parallelTask.get());
        }
    }

    @Test
    public void testStructuredScopeResourceManagement() throws Exception {
        StructuredConcurrencyExample.StructuredTaskScope scope =
            new StructuredConcurrencyExample.StructuredTaskScope();

        // Fork some tasks
        var task1 = scope.fork(() -> "Task 1");
        var task2 = scope.fork(() -> "Task 2");

        scope.join();
        scope.throwIfFailed();

        assertEquals("Task 1", task1.get());
        assertEquals("Task 2", task2.get());

        // Manual close should work
        scope.close();

        // Should not be able to fork after close
        assertThrows(IllegalStateException.class, () -> {
            scope.fork(() -> "Should fail");
        });
    }

    @Test
    public void testLargeNumberOfSubtasks() throws Exception {
        final int numTasks = 1000;

        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            List<StructuredConcurrencyExample.Subtask<Integer>> tasks = new ArrayList<>();

            // Fork many small tasks
            for (int i = 0; i < numTasks; i++) {
                final int taskId = i;
                var task = scope.fork(() -> {
                    Thread.sleep(1); // Minimal work
                    return taskId;
                });
                tasks.add(task);
            }

            scope.join();
            scope.throwIfFailed();

            // Verify all tasks completed
            for (int i = 0; i < numTasks; i++) {
                assertEquals(i, tasks.get(i).get().intValue());
                assertEquals("SUCCESS", tasks.get(i).state());
            }
        }
    }

    @Test
    public void testEmptyScope() throws Exception {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            // Join without forking any tasks
            scope.join();
            scope.throwIfFailed(); // Should not throw

            assertNull(scope.exception());
        }
    }
}
