package org.alxkm.patterns.philosopher;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhilosopherWithSemaphoreTest {
    private static final int PHILOSOPHERS = 5;
    private static final int PERMITS = PHILOSOPHERS - 1;

    /**
     * Seating one fewer philosopher than there are seats is the classic way to keep the table
     * deadlock-free: with at most four of five philosophers competing, someone can always finish.
     * <p>
     * The test asserts the consequence -- every philosopher eventually eats, every thread
     * terminates, and every permit comes back. The previous version asserted nothing whatsoever: it
     * slept two seconds, interrupted the threads and passed no matter what happened, so it could not
     * have detected starvation, a deadlock, or a leaked permit.
     */
    @Test
    void everyPhilosopherEatsAndEveryPermitIsReturned() throws InterruptedException {
        Semaphore semaphore = new Semaphore(PERMITS);
        PhilosopherWithSemaphore[] philosophers = new PhilosopherWithSemaphore[PHILOSOPHERS];

        for (int i = 0; i < PHILOSOPHERS; i++) {
            philosophers[i] = new PhilosopherWithSemaphore(semaphore, "Philosopher " + (i + 1));
            philosophers[i].start();
        }

        for (PhilosopherWithSemaphore philosopher : philosophers) {
            philosopher.join(TimeUnit.SECONDS.toMillis(10));
            assertFalse(philosopher.isAlive(),
                    philosopher.getName() + " never finished, which suggests it is stuck waiting for a permit");
        }

        assertTrue(Arrays.stream(philosophers).allMatch(PhilosopherWithSemaphore::isFull),
                "some philosopher was starved and never ate");
        assertEquals(PERMITS, semaphore.availablePermits(), "permits were leaked or invented");
    }
}
