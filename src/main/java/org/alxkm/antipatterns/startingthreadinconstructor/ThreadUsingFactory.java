package org.alxkm.antipatterns.startingthreadinconstructor;

/**
 *
 * Another approach is to use factory methods to create and start the thread,
 * ensuring separation of concerns and better control over the thread lifecycle.
 *
 * In this version, the ThreadUsingFactory class provides a private constructor
 * and a factory method createAndStart to create and start the thread.
 * This ensures that the thread starts at an appropriate time and provides better encapsulation of the thread creation logic.
 *
 */
public class ThreadUsingFactory {
    private final String message;

    /**
     * Private constructor to enforce usage of factory method.
     *
     * @param message the message to be printed by the thread.
     */
    private ThreadUsingFactory(String message) {
        this.message = message;
    }

    /**
     * Factory method to create and start the thread.
     *
     * @param message the message to be printed by the thread.
     * @return the created thread.
     */
    public static ThreadUsingFactory createAndStart(String message) {
        ThreadUsingFactory thread = new ThreadUsingFactory(message);
        thread.start();
        return thread;
    }

    /**
     * Run method prints the message.
     */
    private void run() {
        System.out.println(message);
    }

    /**
     * Start method to begin thread execution.
     */
    private void start() {
        new Thread(this::run).start();
    }

    public static void main(String[] args) {
        ThreadUsingFactory.createAndStart("Thread started using factory method");
    }
}
