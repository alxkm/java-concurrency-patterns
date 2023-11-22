package ua.com.alxkm.examples.philosopher;

import java.util.concurrent.locks.Lock;

class PhilosopherWithLock extends Thread {
    private final int id;
    private final Lock leftFork;
    private final Lock rightFork;

    public PhilosopherWithLock(int id, Lock leftFork, Lock rightFork) {
        this.id = id;
        this.leftFork = leftFork;
        this.rightFork = rightFork;
    }

    private void think() throws InterruptedException {
        System.out.println("Philosopher " + id + " is thinking");
        Thread.sleep((long) (Math.random() * 1000));
    }

    private void eat() throws InterruptedException {
        System.out.println("Philosopher " + id + " is eating");
        Thread.sleep((long) (Math.random() * 1000));
    }

    private void pickUpForks() {
        leftFork.lock();
        rightFork.lock();
    }

    private void putDownForks() {
        rightFork.unlock();
        leftFork.unlock();
    }

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