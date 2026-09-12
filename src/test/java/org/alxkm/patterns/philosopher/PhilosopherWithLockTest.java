package org.alxkm.patterns.philosopher;

import org.alxkm.diagnostics.DeadlockDetector;
import org.alxkm.testsupport.Await;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhilosopherWithLockTest {
    private static final int PHILOSOPHERS = 5;
    private static final long JOIN_TIMEOUT_MILLIS = 5_000;

    /**
     * Demanding many meals rather than one keeps the table contended long enough that a cyclic
     * lock order would actually close its cycle. Requiring a single meal each is satisfied within
     * milliseconds, before a deadlock has any chance to form, so it would pass against the broken
     * left-then-right version too.
     */
    private static final int MEALS_REQUIRED = 50;

    /**
     * Verifies that the lock-ordered solution to the dining philosophers problem makes progress and
     * shuts down cleanly.
     * <p>
     * Deadlock-freedom is not directly observable, but its consequence is: if the philosophers were
     * deadlocked, none of them would ever finish a meal. So the test waits for every philosopher to
     * eat at least once, which a deadlocked table can never satisfy, and then confirms that all the
     * forks come back unlocked -- proving no thread died holding one.
     * <p>
     * The previous version of this test asserted nothing at all. It slept five seconds, interrupted
     * the threads and passed unconditionally, including against a table that deadlocked instantly.
     */
    @Test
    void everyPhilosopherEatsAndAllForksAreReleased() throws InterruptedException {
        Lock[] forks = new Lock[PHILOSOPHERS];
        Arrays.setAll(forks, i -> new ReentrantLock());

        PhilosopherWithLock[] philosophers = new PhilosopherWithLock[PHILOSOPHERS];
        for (int i = 0; i < PHILOSOPHERS; i++) {
            // Philosopher i sits between fork i and fork (i + 1) % PHILOSOPHERS, closing the ring.
            philosophers[i] = new PhilosopherWithLock(i, i, (i + 1) % PHILOSOPHERS, forks);
        }
        for (PhilosopherWithLock philosopher : philosophers) {
            philosopher.start();
        }

        try {
            Await.until("every philosopher to eat " + MEALS_REQUIRED + " meals",
                    () -> Arrays.stream(philosophers)
                            .allMatch(p -> p.getMealsEaten() >= MEALS_REQUIRED)
                            || DeadlockDetector.deadlockedAmong(philosophers) != null);

            // Ask the JVM directly: this reports any cycle of threads blocked on each other's
            // monitors or ownable synchronizers, which is exactly what fork ordering prevents.
            assertNull(DeadlockDetector.deadlockedAmong(philosophers), "the philosophers deadlocked");
            assertTrue(Arrays.stream(philosophers).allMatch(p -> p.getMealsEaten() >= MEALS_REQUIRED),
                    "philosophers stopped making progress");
        } finally {
            for (PhilosopherWithLock philosopher : philosophers) {
                philosopher.interrupt();
            }
            for (PhilosopherWithLock philosopher : philosophers) {
                philosopher.join(JOIN_TIMEOUT_MILLIS);
            }
        }

        for (PhilosopherWithLock philosopher : philosophers) {
            assertFalse(philosopher.isAlive(), "philosopher did not stop when interrupted");
        }
        for (int i = 0; i < PHILOSOPHERS; i++) {
            ReentrantLock fork = (ReentrantLock) forks[i];
            assertFalse(fork.isLocked(), "fork " + i + " was left locked");
        }
    }
}
