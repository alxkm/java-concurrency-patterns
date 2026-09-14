package org.alxkm.patterns.executors;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
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

    /** Tasks started and not yet finished. Decremented when a task completes, or isTerminated never holds. */
    private final AtomicInteger runningTasks = new AtomicInteger(0);

    private volatile boolean isShutdown = false;

    /** The threads currently running a task, so shutdownNow can actually interrupt them. */
    private final Set<Thread> runningThreads = ConcurrentHashMap.newKeySet();

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
    /**
     * Starts the command on a thread of its own.
     *
     * The bookkeeping around the call is the part worth copying. An earlier version of this class
     * only incremented the counter, so it never returned to zero, isTerminated was permanently false
     * and awaitTermination burned its whole timeout before reporting failure. Whatever a custom
     * executor counts, it has to count both ways.
     *
     * @param command the task to run.
     */
    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        runningTasks.incrementAndGet();
        Thread thread = new Thread(() -> {
            try {
                command.run();
            } finally {
                runningThreads.remove(Thread.currentThread());
                runningTasks.decrementAndGet();
            }
        });
        runningThreads.add(thread);
        thread.start();
    }

    /**
     * Shuts down the executor service.
     */
    @Override
    public void shutdown() {
        isShutdown = true;
    }

    /**
     * Stops accepting work and interrupts whatever is already running.
     *
     * The returned list is always empty, and that is correct rather than a stub: this executor starts
     * every task immediately on its own thread, so nothing is ever queued and there is nothing
     * awaiting execution to hand back. The previous version returned a set that no code ever added
     * to, which looked like the same answer for the wrong reason.
     *
     * @return an empty list; this executor never queues.
     */
    @Override
    public List<Runnable> shutdownNow() {
        isShutdown = true;
        runningThreads.forEach(Thread::interrupt);
        return List.of();
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
        return isShutdown && runningTasks.get() == 0;
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

    public static void main(String[] args) throws InterruptedException {
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
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Retrieve results
        for (Future<Integer> future : futures) {
            try {
                System.out.println("Task result: " + future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                // One task failing should not cost us the results of the others.
                System.out.println("Task failed: " + e.getCause());
            }
        }
    }
}
