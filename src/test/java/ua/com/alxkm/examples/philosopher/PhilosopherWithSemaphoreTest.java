package ua.com.alxkm.examples.philosopher;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;

public class PhilosopherWithSemaphoreTest {
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
