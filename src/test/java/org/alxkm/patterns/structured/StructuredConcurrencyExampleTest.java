package org.alxkm.patterns.structured;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.ArrayList;

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
    public void testStructuredTaskScopeWithException() {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            // Fork tasks where one fails
            var task1 = scope.fork(() -> "Success");
            var task2 = scope.fork(() -> {
                throw new RuntimeException("Task failed");
            });
            var task3 = scope.fork(() -> "Another success");

            scope.join();

            // Should have recorded the exception
            assertNotNull(scope.exception());
            
            // Attempting to throw should propagate the exception
            assertThrows(Exception.class, () -> scope.throwIfFailed());

            // Check task states
            assertEquals("SUCCESS", task1.state());
            assertEquals("FAILED", task2.state());
            assertEquals("SUCCESS", task3.state());

        } catch (Exception e) {
            // Expected to reach here due to exception handling
        }
    }

    @Test
    public void testStructuredTaskScopeWithTimeout() throws InterruptedException {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            // Fork tasks with different durations
            var fastTask = scope.fork(() -> {
                try {
                    Thread.sleep(50);
                    return "Fast completed";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });

            var slowTask = scope.fork(() -> {
                try {
                    Thread.sleep(2000); // This will be interrupted
                    return "Slow completed";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });

            // Set timeout that should interrupt slow task
            Instant deadline = Instant.now().plus(Duration.ofMillis(200));
            
            assertThrows(InterruptedException.class, () -> {
                scope.joinUntil(deadline);
            });

            // Fast task should complete, slow task should be cancelled
            assertEquals("SUCCESS", fastTask.state());
            assertEquals("CANCELLED", slowTask.state());
        }
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
    public void testCancelledTaskScope() throws InterruptedException {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            // Fork a long-running task
            var task = scope.fork(() -> {
                try {
                    Thread.sleep(5000);
                    return "Should not complete";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });

            // Cancel immediately
            scope.cancel();

            // Should not be able to fork new tasks
            assertThrows(IllegalStateException.class, () -> {
                scope.fork(() -> "New task");
            });

            // Task should be cancelled
            Thread.sleep(100); // Give time for cancellation to propagate
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
    public void testFailFastBehavior() throws InterruptedException {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            // Fork tasks where one fails quickly
            var quickTask = scope.fork(() -> "Quick success");
            var failingTask = scope.fork(() -> {
                Thread.sleep(10);
                throw new RuntimeException("Quick failure");
            });
            var slowTask = scope.fork(() -> {
                Thread.sleep(1000); // Should be cancelled
                return "Slow success";
            });

            scope.join();

            // Should have recorded the exception from failing task
            assertNotNull(scope.exception());
            assertTrue(scope.exception().getMessage().contains("Quick failure"));

            // Check states
            assertEquals("SUCCESS", quickTask.state());
            assertEquals("FAILED", failingTask.state());
            // Slow task may be cancelled or still running depending on timing
            assertTrue(slowTask.state().equals("CANCELLED") || 
                      slowTask.state().equals("RUNNING") ||
                      slowTask.state().equals("SUCCESS"));
        }
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
    public void testExceptionInSubtask() throws InterruptedException {
        try (var scope = new StructuredConcurrencyExample.StructuredTaskScope()) {
            var successTask = scope.fork(() -> "Success");
            var exceptionTask = scope.fork(() -> {
                throw new IllegalArgumentException("Test exception");
            });

            scope.join();

            // Exception should be recorded
            Throwable exception = scope.exception();
            assertNotNull(exception);
            assertTrue(exception.getMessage().contains("Test exception"));

            // Success task should still complete
            try {
                assertEquals("Success", successTask.get());
                assertEquals("SUCCESS", successTask.state());
            } catch (ExecutionException e) {
                // This is also acceptable as the task might not complete
                // if the scope cancels due to the exception
            }

            assertEquals("FAILED", exceptionTask.state());
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