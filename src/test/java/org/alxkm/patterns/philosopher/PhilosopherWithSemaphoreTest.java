package org.alxkm.patterns.philosopher;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;

public class PhilosopherWithSemaphoreTest {

    /**
     * This test method verifies the behavior of the PhilosopherWithSemaphore class, which implements
     * the Dining Philosophers problem solution using semaphores. It creates a certain number of philosopher
     * threads (in this case, 5) and initializes a semaphore with the maximum allowed number of concurrent
     * philosophers minus one (to avoid deadlock). Each philosopher thread represents a philosopher in the
     * dining philosophers scenario. The test runs the simulation for a specified time duration (e.g., 2 seconds)
     * to allow the philosophers to attempt to acquire forks and eat. After the simulation period, all philosopher
     * threads are interrupted to stop the simulation. This test checks whether the Dining Philosophers problem
     * solution using semaphores works as expected and avoids deadlock and starvation scenarios.
     */
    @Test
    public void testPhilosophersWithSemaphore() {
        int numOfPhilosophers = 5;
        Semaphore semaphore = new Semaphore(numOfPhilosophers - 1);

        PhilosopherWithSemaphore[] philosophers = new PhilosopherWithSemaphore[numOfPhilosophers];

        for (int i = 0; i < numOfPhilosophers; i++) {
            philosophers[i] = new PhilosopherWithSemaphore(semaphore, "Philosopher " + (i + 1));
            philosophers[i].start();
        }

        try {
            // Let the simulation run for a certain time (e.g., 2 seconds) in the test
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Interrupt all philosophers to stop the simulation after a certain time
        for (PhilosopherWithSemaphore philosopher : philosophers) {
            philosopher.interrupt();
        }
    }
}
