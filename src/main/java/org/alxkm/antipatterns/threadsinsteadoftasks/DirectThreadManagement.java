package org.alxkm.antipatterns.threadsinsteadoftasks;

/**
 * Directly creating and managing threads instead of using the Executor framework can lead to poor scalability and complex error handling.
 * <p>
 * In this example, tasks are executed by directly creating and managing threads.
 * This approach can lead to problems such as unbounded thread creation, poor resource management, and difficulty in handling errors.
 */
public class DirectThreadManagement {
    /**
     * A simple task that prints the thread name.
     */
    public void performTask() {
        Runnable task = () -> {
            System.out.println("Task executed by: " + Thread.currentThread().getName());
        };

        // Directly creating and starting threads
        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DirectThreadManagement manager = new DirectThreadManagement();
        manager.performTask();
    }
}
