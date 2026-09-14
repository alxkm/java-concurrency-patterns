package org.alxkm.patterns.executors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the usage of ThreadPoolExecutor to execute tasks.
 */
public class ThreadPoolExecutorExample {
    /**
     * We create a ThreadPoolExecutor with a core pool size of 2, maximum pool size of 4, and a queue capacity of 10.
     * We submit 10 tasks to the executor using the submit() method.
     * Each task prints its ID and the name of the thread executing it, simulating a task execution time of 1 second.
     * Finally, we shut down the executor using the shutdown() method.
     */
    /** Core, maximum and queue capacity used by the demo in main. */
    private static final int DEMO_CORE = 2;

    private static final int DEMO_MAX = 4;

    private static final int DEMO_QUEUE = 10;

    private static final int DEMO_TASKS = 10;

    private static final long DEMO_WORK_MILLIS = 1000;

    public static void main(String[] args) {
        int threadsUsed = run(DEMO_CORE, DEMO_MAX, DEMO_QUEUE, DEMO_TASKS, DEMO_WORK_MILLIS);
        System.out.println("Ran " + DEMO_TASKS + " tasks on " + threadsUsed + " distinct threads,"
                + " with a maximum pool size of " + DEMO_MAX + ".");
    }

    /**
     * Runs the tasks on a hand-configured pool and reports how many threads it actually used.
     *
     * The answer surprises people, and it is the reason to configure a pool by hand rather than by
     * habit. A ThreadPoolExecutor grows past its core size only when the QUEUE IS FULL, not when work
     * is waiting. With a queue of 10 and 10 tasks, the queue absorbs everything and the maximum pool
     * size never comes into play: the pool stays at its core size and the extra capacity you
     * configured is never used. An unbounded queue makes maximumPoolSize dead configuration entirely.
     *
     * @param corePoolSize    threads kept alive even when idle.
     * @param maximumPoolSize the ceiling, reached only once the queue is full.
     * @param queueCapacity   how many tasks may wait before the pool grows.
     * @param taskCount       how many tasks to submit.
     * @param workMillis      how long each task simulates working for.
     * @return how many distinct threads ran the tasks.
     */
    public static int run(int corePoolSize, int maximumPoolSize, int queueCapacity,
                          int taskCount, long workMillis) {
        Set<Long> threadIds = ConcurrentHashMap.newKeySet();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueCapacity));

        try {
            // Submit tasks to the executor
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    threadIds.add(Thread.currentThread().threadId());
                    System.out.println("Task " + taskId + " executed by thread: " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(workMillis); // Simulate task execution time
                    } catch (InterruptedException e) {
                        // Restore the flag and stop: an interrupt is a request to abandon this task.
                        Thread.currentThread().interrupt();
                        System.err.println("Task " + taskId + " was interrupted");
                    }
                });
            }
        } finally {
            // Shutdown the executor
            executor.shutdown();
            try {
                // Wait for tasks to complete
                if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
        return threadIds.size();
    }
}
