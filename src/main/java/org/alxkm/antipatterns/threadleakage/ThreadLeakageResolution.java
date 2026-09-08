package org.alxkm.antipatterns.threadleakage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the resolution of thread leakage by using a thread pool to manage threads.
 * <p>
 * The same number of tasks as {@link ThreadLeakageExample} runs here, but on a fixed pool of
 * {@value #POOL_SIZE} threads that are reused and then shut down, so the thread count stays flat no matter
 * how much work is submitted.
 */
public class ThreadLeakageResolution {

    private static final int POOL_SIZE = 10;
    private static final int TASK_COUNT = 100;

    private final ExecutorService executorService;

    /**
     * Initializes the ThreadLeakageResolution with a fixed thread pool.
     */
    public ThreadLeakageResolution() {
        // Initialize a fixed thread pool with a fixed number of threads
        this.executorService = Executors.newFixedThreadPool(POOL_SIZE);
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadLeakageResolution example = new ThreadLeakageResolution();
        example.startThreads(TASK_COUNT);

        System.out.println("Ran " + TASK_COUNT + " tasks on at most " + POOL_SIZE + " threads.");
    }

    /**
     * Submits tasks to the thread pool for execution, preventing thread leakage, then shuts the pool down and
     * waits for the tasks already accepted to finish.
     *
     * @param taskCount how many tasks to submit
     * @throws InterruptedException if this thread is interrupted while waiting for the pool to drain
     */
    public void startThreads(int taskCount) throws InterruptedException {
        for (int i = 0; i < taskCount; i++) {
            executorService.submit(() -> {
                // Simulate some work with a sleep
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Shutdown the executor service and wait for the submitted tasks to drain
        executorService.shutdown();
        if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
            executorService.shutdownNow();
        }
    }
}
