package ua.com.alxkm.examples.philosopher;

import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PhilosopherWithLockTest {
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
