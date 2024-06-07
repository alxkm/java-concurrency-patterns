package org.alxkm.patterns.philosopher;

import java.util.concurrent.locks.Lock;

/**
 * The PhilosopherWithLock class represents a philosopher in the dining philosophers problem
 * using locks to synchronize access to the forks.
 */
class PhilosopherWithLock extends Thread {
    private final int id;
    private final Lock leftFork;
    private final Lock rightFork;

    /**
     * Constructs a PhilosopherWithLock with the specified ID and left and right forks.
     *
     * @param id         the ID of the philosopher
     * @param leftFork   the lock representing the left fork
     * @param rightFork  the lock representing the right fork
     */
    public PhilosopherWithLock(int id, Lock leftFork, Lock rightFork) {
        this.id = id;
        this.leftFork = leftFork;
        this.rightFork = rightFork;
    }

    /**
     * Simulates the philosopher thinking.
     *
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private void think() throws InterruptedException {
        System.out.println("Philosopher " + id + " is thinking");
        Thread.sleep((long) (Math.random() * 1000));
    }

    /**
     * Simulates the philosopher eating.
     *
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private void eat() throws InterruptedException {
        System.out.println("Philosopher " + id + " is eating");
        Thread.sleep((long) (Math.random() * 1000));
    }

    /**
     * Picks up both forks.
     */
    private void pickUpForks() {
        leftFork.lock();
        rightFork.lock();
    }

    /**
     * Puts down both forks.
     */
    private void putDownForks() {
        rightFork.unlock();
        leftFork.unlock();
    }

    /**
     * The main behavior of the philosopher thread.
     */
    @Override
    public void run() {
        try {
            while (true) {
                think();
                pickUpForks();
                eat();
                putDownForks();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}