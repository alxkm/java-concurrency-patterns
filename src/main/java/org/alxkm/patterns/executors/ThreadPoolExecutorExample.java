package org.alxkm.patterns.executors;

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
    public static void main(String[] args) {
        // Create a ThreadPoolExecutor with a core pool size of 2, maximum pool size of 4, and a queue capacity of 10
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 0L, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>(10));

        // Submit tasks to the executor
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by thread: " + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000); // Simulate task execution time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        // Shutdown the executor
        executor.shutdown();
    }
}
