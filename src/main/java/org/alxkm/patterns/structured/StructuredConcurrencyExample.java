package org.alxkm.patterns.structured;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Structured Concurrency Examples - Java 21+ Preview Feature
 * 
 * Structured concurrency treats groups of related tasks running in different threads 
 * as a single unit of work, streamlining error handling and cancellation.
 * 
 * Note: This is a preview feature in Java 21. Enable with --enable-preview
 * 
 * This implementation provides equivalent patterns using available Java 21 features
 * while demonstrating the concepts of structured concurrency.
 */
public class StructuredConcurrencyExample {

    /**
     * A stand-in for the JDK's {@code StructuredTaskScope.ShutdownOnFailure}, which is a preview API on
     * Java 21 and would need {@code --enable-preview} everywhere this repository builds.
     * <p>
     * It keeps the three promises that make structured concurrency worth having:
     * <ul>
     *   <li><b>Fail fast.</b> The first subtask to fail shuts the scope down: its siblings are cancelled,
     *       and {@link #join()} returns as soon as that happens, not when the slowest would have
     *       finished.</li>
     *   <li><b>A deadline is a deadline.</b> {@link #joinUntil(Instant)} waits until that instant in total,
     *       however many subtasks there are, and reports running out of time as a
     *       {@link TimeoutException}.</li>
     *   <li><b>Nothing outlives the scope.</b> {@link #close()} cancels what is still running and joins
     *       every subtask's thread before it returns.</li>
     * </ul>
     * Each subtask gets a virtual thread of its own, started directly rather than through an executor.
     * That is not a detail. The JDK 21 virtual-thread executor counts a cancelled task as finished the
     * moment it is cancelled, so its {@code close()} and {@code awaitTermination} return while a task that
     * ignores the interrupt is still running. Joining the threads themselves is the only way to keep the
     * third promise.
     * <p>
     * As with the JDK's version, only the thread that created the scope should fork, join and close it.
     */
    public static class StructuredTaskScope implements AutoCloseable {
        private final ThreadFactory threads = Thread.ofVirtual().name("subtask-", 0).factory();
        // The owner adds to this while a failing subtask, on its own thread, walks it to cancel the rest.
        private final List<Subtask<?>> subtasks = new CopyOnWriteArrayList<>();
        private volatile boolean shutdown;
        private volatile Throwable firstException;
        private volatile Subtask<?> failedSubtask;

        /**
         * Starts a subtask on its own virtual thread.
         *
         * @throws IllegalStateException if a failure, {@link #cancel()} or {@link #close()} has shut the
         *                               scope down.
         */
        public <T> Subtask<T> fork(Callable<T> task) {
            if (shutdown) {
                throw new IllegalStateException("Scope is shut down");
            }
            Subtask<T> subtask = new Subtask<>();
            subtask.start(threads, () -> {
                try {
                    return task.call();
                } catch (Throwable failure) {
                    failed(subtask, failure);
                    throw failure;
                }
            });
            subtasks.add(subtask);
            if (shutdown && subtask != failedSubtask) {
                // A sibling failed between the check above and the add, after its shutdown had already
                // cancelled everything in the list. Without this the new subtask would run to the end.
                subtask.cancel();
            }
            return subtask;
        }

        /**
         * Records the first failure and cancels every other subtask.
         */
        private synchronized void failed(Subtask<?> failing, Throwable failure) {
            if (shutdown) {
                // A sibling failed first, or the scope was cancelled and this subtask is reacting to the
                // interrupt. Either way the scope caused this, and it is not a failure of its own.
                return;
            }
            firstException = failure;
            failedSubtask = failing;
            shutdown = true;
            for (Subtask<?> sibling : subtasks) {
                if (sibling != failing) {
                    sibling.cancel();
                }
            }
        }

        /**
         * Waits until every subtask has finished, or until a failure has shut the scope down.
         */
        public StructuredTaskScope join() throws InterruptedException {
            for (Subtask<?> subtask : subtasks) {
                subtask.await();
            }
            return this;
        }

        /**
         * Like {@link #join()}, but gives up at {@code deadline}, cancelling whatever is still running.
         *
         * @throws TimeoutException if the deadline passes first.
         */
        public StructuredTaskScope joinUntil(Instant deadline) throws InterruptedException, TimeoutException {
            for (Subtask<?> subtask : subtasks) {
                // What is left of the deadline, not the original timeout. Restarting the full timeout for
                // each subtask in turn would let the last of N subtasks run up to N times past it.
                long remaining = Math.max(0, Duration.between(Instant.now(), deadline).toNanos());
                if (!subtask.await(remaining)) {
                    cancel();
                    throw new TimeoutException("Deadline " + deadline + " passed with subtasks still running");
                }
            }
            return this;
        }

        /**
         * Shuts the scope down: cancels every subtask still running and refuses new ones.
         */
        public synchronized void cancel() {
            shutdown = true;
            for (Subtask<?> subtask : subtasks) {
                subtask.cancel();
            }
        }

        /**
         * The first failure, exactly as it was thrown, or {@code null} if nothing failed.
         */
        public Throwable exception() {
            return firstException;
        }

        /**
         * Rethrows the first failure, if there was one.
         */
        public void throwIfFailed() throws Exception {
            Throwable failure = firstException;
            if (failure instanceof Exception exception) {
                throw exception;
            }
            if (failure instanceof Error error) {
                throw error;
            }
        }

        /**
         * Cancels what is still running, then waits for every subtask's thread to finish, however long
         * that takes. Returning earlier would leave a subtask running outside the block that started it.
         */
        @Override
        public void close() {
            cancel();
            boolean interrupted = false;
            for (Subtask<?> subtask : subtasks) {
                interrupted |= subtask.joinThread();
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Represents a subtask within a structured scope.
     */
    public static class Subtask<T> {
        // Set once by the owner before the subtask is shared, through thread start and the scope's list.
        private volatile FutureTask<T> future;
        private volatile Thread thread;

        void start(ThreadFactory threads, Callable<T> body) {
            future = new FutureTask<>(body);
            thread = threads.newThread(future);
            thread.start();
        }

        void cancel() {
            future.cancel(true);
        }

        /** Waits for the outcome, whatever it is. The outcome itself is read through get() or state(). */
        void await() throws InterruptedException {
            try {
                future.get();
            } catch (ExecutionException | CancellationException e) {
                // Finished: failed or cancelled.
            }
        }

        /** @return false if the subtask was still running when the time ran out. */
        boolean await(long nanos) throws InterruptedException {
            try {
                future.get(nanos, TimeUnit.NANOSECONDS);
            } catch (ExecutionException | CancellationException e) {
                // Finished: failed or cancelled.
            } catch (TimeoutException e) {
                return false;
            }
            return true;
        }

        /**
         * Waits for the thread itself to end, which a cancelled subtask may not have done yet.
         *
         * @return whether the calling thread was interrupted while it waited.
         */
        boolean joinThread() {
            boolean interrupted = false;
            while (true) {
                try {
                    thread.join();
                    return interrupted;
                } catch (InterruptedException e) {
                    interrupted = true;   // keep waiting; the caller re-asserts it at the end
                }
            }
        }

        /**
         * Get the result of the subtask.
         */
        public T get() throws ExecutionException, InterruptedException {
            return future.get();
        }

        /**
         * Check if the subtask is done.
         */
        public boolean isDone() {
            return future.isDone();
        }

        /**
         * Get the state of the subtask.
         */
        public String state() {
            if (future.isCancelled()) {
                return "CANCELLED";
            } else if (future.isDone()) {
                try {
                    future.get();
                    return "SUCCESS";
                } catch (Exception e) {
                    return "FAILED";
                }
            } else {
                return "RUNNING";
            }
        }
    }

    /**
     * Example 1: Basic structured concurrency - fetch data from multiple sources.
     */
    public static void basicStructuredConcurrencyExample() {
        System.out.println("=== Basic Structured Concurrency Example ===");
        
        try (var scope = new StructuredTaskScope()) {
            // Fork multiple subtasks
            Subtask<String> userTask = scope.fork(() -> fetchUserData("user123"));
            Subtask<String> orderTask = scope.fork(() -> fetchOrderData("user123"));
            Subtask<String> profileTask = scope.fork(() -> fetchProfileData("user123"));
            
            // Wait for all subtasks to complete
            scope.join();
            scope.throwIfFailed();
            
            // All tasks completed successfully, use results
            String userData = userTask.get();
            String orderData = orderTask.get();
            String profileData = profileTask.get();
            
            System.out.println("User: " + userData);
            System.out.println("Orders: " + orderData);
            System.out.println("Profile: " + profileData);
            
        } catch (Exception e) {
            System.err.println("Failed to fetch data: " + e.getMessage());
        }
    }

    /**
     * Example 2: Structured concurrency with timeout.
     */
    public static void structuredConcurrencyWithTimeout() {
        System.out.println("\n=== Structured Concurrency with Timeout ===");
        
        try (var scope = new StructuredTaskScope()) {
            // Fork tasks that might take too long
            Subtask<String> fastTask = scope.fork(() -> simulateWork("Fast task", 100));
            Subtask<String> slowTask = scope.fork(() -> simulateWork("Slow task", 2000));
            Subtask<String> mediumTask = scope.fork(() -> simulateWork("Medium task", 500));
            
            // Set a timeout
            Instant deadline = Instant.now().plus(Duration.ofMillis(800));
            scope.joinUntil(deadline);
            
            System.out.println("Fast task: " + fastTask.state());
            System.out.println("Medium task: " + mediumTask.state());
            System.out.println("Slow task: " + slowTask.state());
            
        } catch (TimeoutException e) {
            System.out.println("Tasks timed out and were cancelled");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Example 3: Fail-fast structured concurrency.
     */
    public static void failFastExample() {
        System.out.println("\n=== Fail-Fast Structured Concurrency ===");

        long start = System.nanoTime();
        try (var scope = new StructuredTaskScope()) {
            // Fork tasks where one will fail
            Subtask<String> task1 = scope.fork(() -> simulateWork("Task 1", 200));
            Subtask<String> task2 = scope.fork(() -> {
                Thread.sleep(100);
                throw new RuntimeException("Task 2 failed!");
            });
            Subtask<String> task3 = scope.fork(() -> simulateWork("Task 3", 300));
            
            scope.join();
            // Task 2 fails at 100 ms and cancels the other two, so join returns then, not at 300 ms
            // when Task 3 would have finished.
            System.out.println("join returned after " + (System.nanoTime() - start) / 1_000_000 + " ms");
            scope.throwIfFailed();

            // This won't be reached due to the exception
            System.out.println("All tasks completed successfully");
            
        } catch (Exception e) {
            System.out.println("One or more tasks failed: " + e.getMessage());
        }
    }

    /**
     * Example 4: Structured concurrency for parallel processing.
     */
    public static void parallelProcessingExample() {
        System.out.println("\n=== Parallel Processing with Structured Concurrency ===");
        
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger totalSum = new AtomicInteger(0);
        
        try (var scope = new StructuredTaskScope()) {
            // Fork a task for each number to compute its square
            List<Subtask<Integer>> tasks = new ArrayList<>();
            
            for (Integer number : numbers) {
                Subtask<Integer> task = scope.fork(() -> {
                    // Simulate some computation
                    Thread.sleep(50);
                    return number * number;
                });
                tasks.add(task);
            }
            
            scope.join();
            scope.throwIfFailed();
            
            // Collect results
            for (Subtask<Integer> task : tasks) {
                totalSum.addAndGet(task.get());
            }
            
            System.out.println("Sum of squares: " + totalSum.get());
            
        } catch (Exception e) {
            System.err.println("Parallel processing failed: " + e.getMessage());
        }
    }

    /**
     * Example 5: Structured concurrency for service orchestration.
     */
    public static void serviceOrchestrationExample() {
        System.out.println("\n=== Service Orchestration Example ===");
        
        try (var scope = new StructuredTaskScope()) {
            // Fork multiple service calls
            Subtask<String> authService = scope.fork(() -> callAuthService("token123"));
            Subtask<String> dataService = scope.fork(() -> callDataService("query"));
            Subtask<String> cacheService = scope.fork(() -> callCacheService("key"));
            
            scope.join();
            scope.throwIfFailed();
            
            // Orchestrate the results
            String authResult = authService.get();
            String dataResult = dataService.get();
            String cacheResult = cacheService.get();
            
            String orchestratedResult = String.format(
                "Orchestrated result: Auth=%s, Data=%s, Cache=%s",
                authResult, dataResult, cacheResult
            );
            
            System.out.println(orchestratedResult);
            
        } catch (Exception e) {
            System.err.println("Service orchestration failed: " + e.getMessage());
        }
    }

    /**
     * Example 6: Nested structured concurrency.
     */
    public static void nestedStructuredConcurrencyExample() {
        System.out.println("\n=== Nested Structured Concurrency ===");
        
        try (var outerScope = new StructuredTaskScope()) {
            
            // Fork a task that itself uses structured concurrency
            Subtask<String> complexTask = outerScope.fork(() -> {
                try (var innerScope = new StructuredTaskScope()) {
                    Subtask<String> subTask1 = innerScope.fork(() -> simulateWork("SubTask 1", 100));
                    Subtask<String> subTask2 = innerScope.fork(() -> simulateWork("SubTask 2", 150));
                    
                    innerScope.join();
                    innerScope.throwIfFailed();
                    
                    return "Complex task result: " + subTask1.get() + ", " + subTask2.get();
                }
            });
            
            // Fork other parallel tasks
            Subtask<String> simpleTask = outerScope.fork(() -> simulateWork("Simple task", 80));
            
            outerScope.join();
            outerScope.throwIfFailed();
            
            System.out.println(complexTask.get());
            System.out.println(simpleTask.get());
            
        } catch (Exception e) {
            System.err.println("Nested structured concurrency failed: " + e.getMessage());
        }
    }

    // Utility methods to simulate various operations
    
    private static String fetchUserData(String userId) {
        try {
            Thread.sleep(150);
            return "User data for " + userId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String fetchOrderData(String userId) {
        try {
            Thread.sleep(200);
            return "Order data for " + userId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String fetchProfileData(String userId) {
        try {
            Thread.sleep(100);
            return "Profile data for " + userId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String simulateWork(String taskName, long durationMs) {
        try {
            Thread.sleep(durationMs);
            return taskName + " completed in " + durationMs + "ms";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String callAuthService(String token) {
        try {
            Thread.sleep(120);
            return "Auth validated for " + token;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String callDataService(String query) {
        try {
            Thread.sleep(180);
            return "Data retrieved for " + query;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String callCacheService(String key) {
        try {
            Thread.sleep(90);
            return "Cache hit for " + key;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Main method demonstrating all structured concurrency examples.
     */
    public static void main(String[] args) {
        System.out.println("Structured Concurrency Examples (Java 21+)");
        System.out.println("Running on Java version: " + System.getProperty("java.version"));
        
        try {
            basicStructuredConcurrencyExample();
            structuredConcurrencyWithTimeout();
            failFastExample();
            parallelProcessingExample();
            serviceOrchestrationExample();
            nestedStructuredConcurrencyExample();
            
            System.out.println("\nAll structured concurrency examples completed!");
            
        } catch (Exception e) {
            System.err.println("Error running structured concurrency examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
}