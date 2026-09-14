package org.alxkm.patterns.executors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages a pool of worker threads to perform tasks concurrently.
 * The ThreadPoolExample class demonstrates the use of an ExecutorService to manage a fixed thread pool.
 */
public class ThreadPoolExample {
    /**
     * The main method of the example program.
     * Creates a fixed thread pool with 10 threads and submits 20 tasks to be executed concurrently.
     * The ExecutorService is then shut down to stop accepting new tasks.
     *
     * @param args The command-line arguments (unused).
     */
    /** Pool size used by the demo in main. */
    private static final int DEMO_POOL_SIZE = 10;

    /** Task count used by the demo in main. */
    private static final int DEMO_TASKS = 20;

    public static void main(String[] args) throws InterruptedException {
        int threadsUsed = run(DEMO_POOL_SIZE, DEMO_TASKS);
        System.out.println("Ran " + DEMO_TASKS + " tasks on " + threadsUsed + " distinct threads.");
    }

    /**
     * Submits the tasks to a fixed pool and waits for them to finish.
     *
     * The number of threads is the thing worth noticing: it is bounded by the pool, not by the number
     * of tasks, so twenty tasks on a pool of ten never produce twenty threads.
     *
     * @param poolSize  how many threads the pool keeps.
     * @param taskCount how many tasks to submit.
     * @return how many distinct threads ran the tasks, never more than poolSize.
     * @throws InterruptedException if this thread is interrupted while waiting for the pool to drain.
     */
    public static int run(int poolSize, int taskCount) throws InterruptedException {
        Set<Long> threadIds = ConcurrentHashMap.newKeySet();
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        for (int i = 0; i < taskCount; i++) {
            Task task = new Task(i);
            executorService.execute(() -> {
                threadIds.add(Thread.currentThread().threadId());
                task.run();
            });
        }

        executorService.shutdown();
        if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
            executorService.shutdownNow();
            throw new IllegalStateException("pool did not drain");
        }
        return threadIds.size();
    }
}
