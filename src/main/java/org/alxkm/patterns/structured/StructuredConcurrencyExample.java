package org.alxkm.patterns.structured;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
     * Custom structured task scope for managing related tasks.
     */
    public static class StructuredTaskScope implements AutoCloseable {
        private final ExecutorService executor;
        private final List<Future<?>> futures;
        private volatile boolean cancelled = false;
        private volatile Throwable firstException = null;
        
        public StructuredTaskScope() {
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            this.futures = new ArrayList<>();
        }
        
        /**
         * Fork a new subtask.
         */
        public <T> Subtask<T> fork(Callable<T> task) {
            if (cancelled) {
                throw new IllegalStateException("Scope is cancelled");
            }
            
            Future<T> future = executor.submit(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    synchronized (this) {
                        if (firstException == null) {
                            firstException = e;
                        }
                    }
                    throw new RuntimeException(e);
                }
            });
            
            futures.add(future);
            return new Subtask<>(future);
        }
        
        /**
         * Join all subtasks and wait for completion.
         */
        public StructuredTaskScope join() throws InterruptedException {
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    // Exception already recorded
                }
            }
            return this;
        }
        
        /**
         * Join with timeout.
         */
        public StructuredTaskScope joinUntil(Instant deadline) throws InterruptedException {
            Duration timeout = Duration.between(Instant.now(), deadline);
            
            for (Future<?> future : futures) {
                try {
                    future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                } catch (ExecutionException e) {
                    // Exception already recorded
                } catch (TimeoutException e) {
                    cancel();
                    throw new InterruptedException("Timeout exceeded");
                }
            }
            return this;
        }
        
        /**
         * Cancel all subtasks.
         */
        public void cancel() {
            cancelled = true;
            for (Future<?> future : futures) {
                future.cancel(true);
            }
        }
        
        /**
         * Check if any subtask failed.
         */
        public Throwable exception() {
            return firstException;
        }
        
        /**
         * Throw exception if any subtask failed.
         */
        public void throwIfFailed() throws Exception {
            if (firstException instanceof Exception) {
                throw (Exception) firstException;
            } else if (firstException instanceof RuntimeException) {
                throw (RuntimeException) firstException;
            } else if (firstException instanceof Error) {
                throw (Error) firstException;
            }
        }
        
        @Override
        public void close() {
            cancel();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Represents a subtask within a structured scope.
     */
    public static class Subtask<T> {
        private final Future<T> future;
        
        public Subtask(Future<T> future) {
            this.future = future;
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
            
        } catch (InterruptedException e) {
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
        
        try (var scope = new StructuredTaskScope()) {
            // Fork tasks where one will fail
            Subtask<String> task1 = scope.fork(() -> simulateWork("Task 1", 200));
            Subtask<String> task2 = scope.fork(() -> {
                Thread.sleep(100);
                throw new RuntimeException("Task 2 failed!");
            });
            Subtask<String> task3 = scope.fork(() -> simulateWork("Task 3", 300));
            
            scope.join();
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