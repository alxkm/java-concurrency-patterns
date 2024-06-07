package org.alxkm.patterns.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10); // Create a fixed thread pool with 10 threads
        for (int i = 0; i < 20; i++) {
            executorService.execute(new Task(i)); // Submit 20 tasks to the executor service
        }
        executorService.shutdown(); // Shut down the executor service
    }
}

/**
 * Represents a task that can be executed by a thread.
 * The Task class implements the Runnable interface and prints its task ID and the name of the executing thread.
 */
class Task implements Runnable {
    private int taskId; // The ID of the task

    /**
     * Constructs a new Task with the specified task ID.
     *
     * @param taskId The ID of the task.
     */
    public Task(int taskId) {
        this.taskId = taskId;
    }

    /**
     * The run method contains the code to be executed when the task is run.
     * It prints the task ID and the name of the thread that is executing the task.
     */
    @Override
    public void run() {
        System.out.println("Task ID : " + this.taskId + " performed by " + Thread.currentThread().getName());
    }
}
