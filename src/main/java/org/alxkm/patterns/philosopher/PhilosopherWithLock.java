package org.alxkm.patterns.philosopher;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/**
 * The PhilosopherWithLock class represents a philosopher in the dining philosophers problem
 * using locks to synchronize access to the forks.
 * <p>
 * The forks are acquired in a global order -- always the lower-numbered one first -- which is what
 * keeps this solution deadlock-free. Picking up "left then right" instead would let all five
 * philosophers hold their left fork and wait forever for their right, closing a cycle in the
 * wait-for graph. Imposing a total order on the resources removes the cycle, and with it the
 * possibility of deadlock, without any timeout or retry.
 */
public class PhilosopherWithLock extends Thread {
    private final int id;
    private final Lock firstFork;
    private final Lock secondFork;
    private final AtomicInteger mealsEaten = new AtomicInteger();

    /**
     * Constructs a PhilosopherWithLock seated between two numbered forks.
     *
     * @param id          the ID of the philosopher.
     * @param leftForkId  the index of the fork to the philosopher's left.
     * @param rightForkId the index of the fork to the philosopher's right.
     * @param forks       the shared table of forks, indexed by fork ID.
     */
    public PhilosopherWithLock(int id, int leftForkId, int rightForkId, Lock[] forks) {
        this.id = id;
        // Order the two forks by index rather than by hand, so no philosopher is a special case.
        this.firstFork = forks[Math.min(leftForkId, rightForkId)];
        this.secondFork = forks[Math.max(leftForkId, rightForkId)];
    }

    /**
     * Returns how many times this philosopher has finished a meal.
     * <p>
     * Progress is the observable consequence of being deadlock-free, so this is what a test can
     * actually assert on.
     *
     * @return the number of completed meals.
     */
    public int getMealsEaten() {
        return mealsEaten.get();
    }

    /**
     * Simulates the philosopher thinking, holding no forks.
     *
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private void think() throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextLong(1, 4));
    }

    /**
     * Simulates the philosopher eating.
     *
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private void eat() throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextLong(1, 4));
        mealsEaten.incrementAndGet();
    }

    /**
     * Picks up both forks, lower-numbered one first.
     */
    private void pickUpForks() {
        firstFork.lock();
        secondFork.lock();
    }

    /**
     * Puts down both forks, in the reverse of the order they were taken.
     */
    private void putDownForks() {
        secondFork.unlock();
        firstFork.unlock();
    }

    /**
     * The main behavior of the philosopher thread: think, eat, repeat until interrupted.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                think();
                pickUpForks();
                try {
                    eat();
                } finally {
                    // Release the forks even if eating is interrupted, or the neighbours starve.
                    putDownForks();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
