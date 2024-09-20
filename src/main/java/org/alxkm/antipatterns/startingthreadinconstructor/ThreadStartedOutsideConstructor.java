package org.alxkm.antipatterns.startingthreadinconstructor;

/**
 *
 * To resolve this issue, start the thread outside the constructor,
 * ensuring that the object is fully constructed before the thread starts.
 *
 * In this revised example, the thread is created in the main method after the object is fully constructed,
 * ensuring that the thread starts at an appropriate time.
 *
 */
public class ThreadStartedOutsideConstructor extends Thread {
    private final String message;

    /**
     * Constructor initializes the message.
     *
     * @param message the message to be printed by the thread.
     */
    public ThreadStartedOutsideConstructor(String message) {
        this.message = message;
    }

    /**
     * Run method prints the message.
     */
    @Override
    public void run() {
        System.out.println(message);
    }

    public static void main(String[] args) {
        ThreadStartedOutsideConstructor thread = new ThreadStartedOutsideConstructor("Thread started outside constructor");
        thread.start(); // Starting thread outside the constructor
    }
}

