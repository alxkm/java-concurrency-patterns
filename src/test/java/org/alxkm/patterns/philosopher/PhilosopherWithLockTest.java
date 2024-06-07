package org.alxkm.patterns.philosopher;

import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PhilosopherWithLockTest {

    /**
     * This test method verifies the behavior of the PhilosopherWithLock class, which implements
     * the Dining Philosophers problem solution using locks (ReentrantLocks). It creates a certain
     * number of philosopher threads (in this case, 5) and initializes an array of locks to represent
     * the forks on the table. Each philosopher thread represents a philosopher in the dining philosophers
     * scenario and is initialized with the corresponding left and right forks. The test runs the simulation
     * for a specified time duration (e.g., 5 seconds) to allow the philosophers to attempt to acquire forks
     * and eat. After the simulation period, all philosopher threads are interrupted to stop the simulation.
     * This test checks whether the Dining Philosophers problem solution using locks works as expected and
     * avoids deadlock and starvation scenarios.
     */
    @Test
    public void testDiningPhilosophers() {
        int numOfPhilosophers = 5;
        PhilosopherWithLock[] philosopherWithLocks = new PhilosopherWithLock[numOfPhilosophers];
        Lock[] forks = new Lock[numOfPhilosophers];

        for (int i = 0; i < numOfPhilosophers; i++) {
            forks[i] = new ReentrantLock();
        }

        for (int i = 0; i < numOfPhilosophers; i++) {
            philosopherWithLocks[i] = new PhilosopherWithLock(i, forks[i], forks[(i + 1) % numOfPhilosophers]);
            philosopherWithLocks[i].start();
        }

        try {
            // Let the simulation run for a certain time (e.g., 5 seconds) in the test
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Interrupt all philosophers to stop the simulation after a certain time
        for (PhilosopherWithLock philosopherWithLock : philosopherWithLocks) {
            philosopherWithLock.interrupt();
        }
    }
}
