package org.alxkm.antipatterns.deadlock;

/**
 * Example demonstrating a deadlock situation.
 */
public class DeadlockExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    /**
     * Main method to run the deadlock example.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        DeadlockExample example = new DeadlockExample();
        example.causeDeadlock();
    }

    /**
     * Method that creates and starts two threads which will cause a deadlock.
     */
    public void causeDeadlock() {
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread 1: Holding lock 1...");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Thread 1: Waiting for lock 2...");

                synchronized (lock2) {
                    System.out.println("Thread 1: Holding lock 1 & 2...");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("Thread 2: Holding lock 2...");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Thread 2: Waiting for lock 1...");

                synchronized (lock1) {
                    System.out.println("Thread 2: Holding lock 1 & 2...");
                }
            }
        });

        t1.start();
        t2.start();
    }
}
