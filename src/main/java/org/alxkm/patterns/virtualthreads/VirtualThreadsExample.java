package org.alxkm.patterns.virtualthreads;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Virtual Threads Pattern Examples - Java 21+
 * 
 * Virtual threads (Project Loom) provide lightweight threads that are managed by the JVM.
 * They allow for massive concurrency with minimal overhead compared to platform threads.
 * Virtual threads are particularly useful for I/O-bound tasks.
 */
public class VirtualThreadsExample {

    /** Task count used by the no-argument demo overloads and by main. */
    private static final int DEMO_TASKS = 10_000;

    /** How long each demo task blocks for, in milliseconds. */
    private static final int DEMO_SLEEP_MILLIS = 100;

    /** Item count for the producer-consumer demo. */
    private static final int DEMO_ITEMS = 1000;

    /** Task count for the massive-concurrency demo. */
    private static final int DEMO_MASSIVE_TASKS = 100_000;

    /**
     * Basic virtual thread creation and execution.
     *
     * @return true if the work really ran on a virtual thread.
     */
    public static boolean basicVirtualThreadExample() {
        AtomicBoolean ranOnVirtualThread = new AtomicBoolean();
        System.out.println("=== Basic Virtual Thread Example ===");
        
        // Create and start a virtual thread
        Thread virtualThread = Thread.ofVirtual()
                .name("virtual-worker")
                .start(() -> {
                    ranOnVirtualThread.set(Thread.currentThread().isVirtual());
                    System.out.println("Running in virtual thread: " + Thread.currentThread());
                    try {
                        Thread.sleep(1000); // Simulate I/O operation
                        System.out.println("Virtual thread completed work");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

        try {
            virtualThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ranOnVirtualThread.get();
    }

    /**
     * How long the same batch of blocking tasks took on each kind of thread.
     *
     * @param platformMillis elapsed time using a fixed pool of platform threads.
     * @param virtualMillis  elapsed time using one virtual thread per task.
     */
    public record Timings(long platformMillis, long virtualMillis) {
    }

    /**
     * Comparing platform threads vs virtual threads on blocking work, at the demo size.
     *
     * @return the two timings.
     */
    public static Timings performanceComparison() {
        return performanceComparison(DEMO_TASKS, DEMO_SLEEP_MILLIS);
    }

    /**
     * Comparing platform threads vs virtual threads on blocking work.
     *
     * The gap comes from what blocking costs. A platform thread parked in sleep holds an OS thread, so
     * the fixed pool keeps only 200 tasks in flight at a time. A virtual thread parked in sleep releases
     * its carrier, so every task is in flight at once.
     *
     * @param numTasks      how many tasks to run on each kind of thread.
     * @param sleepDuration how long each task blocks, in milliseconds.
     * @return the two timings.
     */
    public static Timings performanceComparison(int numTasks, int sleepDuration) {
        System.out.println("\n=== Performance Comparison ===");
        

        // Test with platform threads
        Instant start = Instant.now();
        try (ExecutorService platformExecutor = Executors.newFixedThreadPool(200)) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < numTasks; i++) {
                futures.add(platformExecutor.submit(() -> {
                    try {
                        Thread.sleep(sleepDuration);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
            
            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throw new IllegalStateException("platform task failed", e);
                }
            }
        }
        Duration platformTime = Duration.between(start, Instant.now());
        
        // Test with virtual threads
        start = Instant.now();
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < numTasks; i++) {
                futures.add(virtualExecutor.submit(() -> {
                    try {
                        Thread.sleep(sleepDuration);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
            
            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throw new IllegalStateException("virtual task failed", e);
                }
            }
        }
        Duration virtualTime = Duration.between(start, Instant.now());
        
        System.out.printf("Platform threads time: %d ms%n", platformTime.toMillis());
        System.out.printf("Virtual threads time: %d ms%n", virtualTime.toMillis());
        System.out.printf("Virtual threads are %.2fx faster%n", 
            (double) platformTime.toMillis() / Math.max(1, virtualTime.toMillis()));

        return new Timings(platformTime.toMillis(), virtualTime.toMillis());
    }

    /**
     * Producer-Consumer pattern with virtual threads, at the demo size.
     *
     * @return the number of items consumed.
     */
    public static int producerConsumerWithVirtualThreads() {
        return producerConsumerWithVirtualThreads(DEMO_ITEMS);
    }

    /**
     * Producer-Consumer pattern with virtual threads.
     *
     * @param totalItems how many items the producer publishes.
     * @return the number of items consumed, which should equal totalItems.
     */
    public static int producerConsumerWithVirtualThreads(int totalItems) {
        System.out.println("\n=== Producer-Consumer with Virtual Threads ===");
        
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);

        // Create virtual thread producer
        Thread producer = Thread.ofVirtual()
                .name("virtual-producer")
                .start(() -> {
                    try {
                        for (int i = 0; i < totalItems; i++) {
                            queue.put("Item-" + i);
                            produced.incrementAndGet();
                            Thread.sleep(1); // Simulate production time
                        }
                        queue.put("POISON_PILL"); // Signal end
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

        // Create multiple virtual thread consumers
        List<Thread> consumers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int consumerId = i;
            Thread consumer = Thread.ofVirtual()
                    .name("virtual-consumer-" + consumerId)
                    .start(() -> {
                        try {
                            while (true) {
                                String item = queue.take();
                                if ("POISON_PILL".equals(item)) {
                                    queue.put(item); // Re-add for other consumers
                                    break;
                                }
                                consumed.incrementAndGet();
                                Thread.sleep(2); // Simulate processing time
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
            consumers.add(consumer);
        }

        try {
            producer.join();
            for (Thread consumer : consumers) {
                consumer.join();
            }
            System.out.printf("Produced: %d, Consumed: %d%n", produced.get(), consumed.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return consumed.get();
    }

    /**
     * Web server simulation using virtual threads.
     */
    public static void webServerSimulation() {
        System.out.println("\n=== Web Server Simulation with Virtual Threads ===");
        
        final int numRequests = 1000;
        AtomicInteger completedRequests = new AtomicInteger(0);
        
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            Instant start = Instant.now();
            
            List<Future<String>> futures = IntStream.range(0, numRequests)
                    .mapToObj(i -> virtualExecutor.submit(() -> handleRequest(i)))
                    .toList();
            
            // Process all requests
            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    completedRequests.incrementAndGet();
                    if (completedRequests.get() % 100 == 0) {
                        System.out.printf("Completed %d requests%n", completedRequests.get());
                    }
                } catch (ExecutionException e) {
                    System.err.println("Request failed: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Request interrupted: " + e.getMessage());
                }
            }
            
            Duration totalTime = Duration.between(start, Instant.now());
            System.out.printf("Completed %d requests in %d ms%n", 
                completedRequests.get(), totalTime.toMillis());
            System.out.printf("Throughput: %.2f requests/second%n", 
                (double) completedRequests.get() / totalTime.toSeconds());
        }
    }

    /**
     * Simulates handling an HTTP request with database and external service calls.
     */
    private static String handleRequest(int requestId) {
        try {
            // Simulate database query
            Thread.sleep(20);
            
            // Simulate external service call
            Thread.sleep(30);
            
            // Simulate response processing
            Thread.sleep(10);
            
            return "Response for request " + requestId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Failed request " + requestId;
        }
    }

    /**
     * Demonstrates virtual thread pools and task execution.
     */
    public static int virtualThreadPoolExample() {
        System.out.println("\n=== Virtual Thread Pool Example ===");
        
        // Create a custom virtual thread factory
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("custom-virtual-", 0)
                .factory();

        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory)) {
            List<Future<Integer>> futures = new ArrayList<>();
            
            // Submit CPU-intensive and I/O tasks
            for (int i = 0; i < 50; i++) {
                final int taskId = i;
                Future<Integer> future = executor.submit(() -> {
                    // Mix of CPU and I/O work
                    int result = 0;
                    for (int j = 0; j < 1000; j++) {
                        result += j;
                    }
                    
                    try {
                        Thread.sleep(10); // Simulate I/O
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    return result + taskId;
                });
                futures.add(future);
            }
            
            // Collect results
            int totalResult = 0;
            for (Future<Integer> future : futures) {
                try {
                    totalResult += future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throw new IllegalStateException("pooled task failed", e);
                }
            }

            System.out.println("Total result from all tasks: " + totalResult);
            return totalResult;
        }
    }

    /**
     * Demonstrates virtual threads with CompletableFuture.
     */
    public static String virtualThreadsWithCompletableFuture() {
        System.out.println("\n=== Virtual Threads with CompletableFuture ===");
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Chain of asynchronous operations using virtual threads
            CompletableFuture<String> result = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            Thread.sleep(100);
                            return "Step 1 completed";
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return "Step 1 failed";
                        }
                    }, executor)
                    .thenApplyAsync(step1Result -> {
                        try {
                            Thread.sleep(100);
                            return step1Result + " -> Step 2 completed";
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return step1Result + " -> Step 2 failed";
                        }
                    }, executor)
                    .thenApplyAsync(step2Result -> {
                        try {
                            Thread.sleep(100);
                            return step2Result + " -> Step 3 completed";
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return step2Result + " -> Step 3 failed";
                        }
                    }, executor);
            
            try {
                String finalResult = result.get(10, TimeUnit.SECONDS);
                System.out.println("CompletableFuture result: " + finalResult);
                return finalResult;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while composing the chain", e);
            } catch (TimeoutException | ExecutionException e) {
                throw new IllegalStateException("CompletableFuture chain failed", e);
            }
        }
    }

    /**
     * Demonstrates massive concurrency with virtual threads, at the demo size.
     *
     * @return the number of tasks that completed.
     */
    public static int massiveConcurrencyExample() {
        return massiveConcurrencyExample(DEMO_MASSIVE_TASKS);
    }

    /**
     * Demonstrates massive concurrency with virtual threads.
     *
     * @param numTasks how many tasks to run concurrently.
     * @return the number of tasks that completed, which should equal numTasks.
     */
    public static int massiveConcurrencyExample(int numTasks) {
        System.out.println("\n=== Massive Concurrency Example ===");
        
        AtomicInteger completedTasks = new AtomicInteger(0);
        
        Instant start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < numTasks; i++) {
                final int taskId = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        // Simulate some work
                        Thread.sleep(1);
                        completedTasks.incrementAndGet();
                        
                        if (taskId % 10_000 == 0) {
                            System.out.printf("Completed %d/%d tasks%n", 
                                completedTasks.get(), numTasks);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                futures.add(future);
            }
            
            // Wait for all tasks
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throw new IllegalStateException("task failed", e);
                }
            }
        }

        Duration totalTime = Duration.between(start, Instant.now());
        System.out.printf("Completed %d virtual threads in %d ms%n", 
            completedTasks.get(), totalTime.toMillis());
        System.out.printf("Average time per task: %.3f ms%n",
            (double) totalTime.toMillis() / numTasks);

        return completedTasks.get();
    }

    /**
     * Main method demonstrating all virtual thread examples.
     */
    public static void main(String[] args) {
        System.out.println("Java Virtual Threads Examples (Java 21+)");
        System.out.println("Running on Java version: " + System.getProperty("java.version"));
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        
        try {
            basicVirtualThreadExample();
            performanceComparison();
            producerConsumerWithVirtualThreads();
            webServerSimulation();
            virtualThreadPoolExample();
            virtualThreadsWithCompletableFuture();
            massiveConcurrencyExample();
            
            System.out.println("\nAll virtual thread examples completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error running virtual thread examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
}