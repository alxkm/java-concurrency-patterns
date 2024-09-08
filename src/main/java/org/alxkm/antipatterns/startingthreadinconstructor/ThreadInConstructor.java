package org.alxkm.antipatterns.startingthreadinconstructor;

/**
 *
 * Starting a thread within a constructor can lead to unpredictable behavior or issues where the thread starts before the object is fully constructed.
 *
 * In this example, the ThreadInConstructor class starts the thread within the constructor.
 * This can lead to issues where the thread starts before the object is fully initialized, potentially causing unexpected behavior.
 *
 */
public class ThreadInConstructor extends Thread {
    private final String message;

    /**
     * Constructor starts the thread immediately.
     * This is problematic as the object might not be fully constructed.
     *
     * @param message the message to be printed by the thread.
     */
    public ThreadInConstructor(String message) {
        this.message = message;
        start(); // Starting thread in the constructor
    }

    /**
     * Run method prints the message.
     */
    @Override
    public void run() {
        System.out.println(message);
    }

    public static void main(String[] args) {
        new ThreadInConstructor("Thread started in constructor");
    }
}

