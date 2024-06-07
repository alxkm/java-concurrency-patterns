package org.alxkm.antipatterns.deadlock;

/**
 * Example demonstrating the resolution of a deadlock situation.
 */
public class DeadlockResolutionExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    /**
     * Main method to run the deadlock resolution example.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        DeadlockResolutionExample example = new DeadlockResolutionExample();
        example.resolveDeadlock();
    }

    /**
     * Method that creates and starts two threads which will avoid deadlock by acquiring locks in the same order.
     */
    public void resolveDeadlock() {
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
            synchronized (lock1) { // Changed the order of acquiring locks
                System.out.println("Thread 2: Holding lock 1...");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Thread 2: Waiting for lock 2...");

                synchronized (lock2) {
                    System.out.println("Thread 2: Holding lock 1 & 2...");
                }
            }
        });

        t1.start();
        t2.start();
    }
}
