package org.alxkm.patterns.executors;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the usage of AbstractExecutorService in a custom ExecutorService implementation.
 *
 *
 * We extend the AbstractExecutorService class and implement its abstract methods: submit, execute, shutdown, isShutdown, isTerminated, and awaitTermination.
 * In the submit method, we create a FutureTask to execute the provided Callable task and return it.
 * In the execute method, we create a new thread to execute the provided Runnable task.
 * The shutdown method sets the isShutdown flag to true.
 * The isShutdown method returns the value of the isShutdown flag.
 * The isTerminated method returns true if the executor service has been shut down and all tasks have completed.
 * The awaitTermination method waits for termination of all tasks after shutdown.
 * In the main method, we create an instance of AbstractExecutorServiceExample, submit tasks, shutdown the executor, and wait for termination.
 * Finally, we retrieve and print the results of the submitted tasks.
 *
 */
public class AbstractExecutorServiceExample extends AbstractExecutorService {

    private final AtomicInteger taskCount = new AtomicInteger(0);
    private volatile boolean isShutdown = false;
    private final ConcurrentSkipListSet<Runnable> tasks = new ConcurrentSkipListSet<>();

    /**
     * Submits a task for execution.
     *
     * @param task the task to submit
     * @param <T>  the result type of the task
     * @return a Future representing the result of the task
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        FutureTask<T> futureTask = new FutureTask<>(task);
        execute(futureTask);
        return futureTask;
    }

    /**
     * Executes the given task.
     *
     * @param command the task to execute
     */
    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        Thread thread = new Thread(command);
        thread.start();
        taskCount.incrementAndGet();
    }

    /**
     * Shuts down the executor service.
     */
    @Override
    public void shutdown() {
        isShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        return tasks.stream().toList();
    }

    /**
     * Returns true if the executor service has been shut down.
     *
     * @return true if the executor service has been shut down, otherwise false
     */
    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    /**
     * Returns true if all tasks have completed after shutdown.
     *
     * @return true if all tasks have completed after shutdown, otherwise false
     */
    @Override
    public boolean isTerminated() {
        return isShutdown && taskCount.get() == 0;
    }

    /**
     * Waits for termination of all tasks after shutdown.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return true if all tasks have completed, false if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        while (!isTerminated() && nanos > 0) {
            long start = System.nanoTime();
            TimeUnit.NANOSECONDS.sleep(Math.min(1000000, nanos)); // sleep for at most 1 millisecond
            nanos -= (System.nanoTime() - start);
        }
        return isTerminated();
    }

    public static void main(String[] args) {
        // Create an instance of AbstractExecutorServiceExample
        AbstractExecutorServiceExample executor = new AbstractExecutorServiceExample();

        // Submit tasks
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            futures.add(executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by thread: " + Thread.currentThread().getName());
                return taskId;
            }));
        }

        // Shutdown the executor
        executor.shutdown();

        // Wait for termination
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Retrieve results
        for (Future<Integer> future : futures) {
            try {
                System.out.println("Task result: " + future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
