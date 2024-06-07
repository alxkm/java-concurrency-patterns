package org.alxkm.antipatterns.threadleakage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demonstrates the resolution of thread leakage by using a thread pool to manage threads.
 */
public class ThreadLeakageResolution {

    private final ExecutorService executorService;

    /**
     * Initializes the ThreadLeakageResolution with a fixed thread pool.
     */
    public ThreadLeakageResolution() {
        // Initialize a fixed thread pool with a fixed number of threads
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public static void main(String[] args) {
        ThreadLeakageResolution example = new ThreadLeakageResolution();
        example.startThreads();
    }

    /**
     * Submits tasks to the thread pool for execution, preventing thread leakage.
     * Submits a fixed number of tasks to the executor service for demonstration purposes.
     */
    public void startThreads() {
        for (int i = 0; i < 100; i++) { // Submitting a fixed number of tasks for demonstration
            executorService.submit(() -> {
                // Simulate some work with a sleep
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Shutdown the executor service after tasks are completed
        executorService.shutdown();
    }
}
