package org.alxkm.patterns.executors;

/**
 * Represents a task that can be executed by a thread.
 * The Task class implements the Runnable interface and prints its task ID and the name of the executing thread.
 */
class Task implements Runnable {
    private final int taskId; // The ID of the task

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
