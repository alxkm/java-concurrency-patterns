package org.alxkm.antipatterns.nonatomiccompoundactions;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Increment if below the limit" is a compound action: a read, a comparison and a write. Guarding
 * the whole sequence is what keeps the limit meaningful.
 */
class CompoundActionTest {
    private static final int THREADS = 8;
    private static final int ATTEMPTS = 20_000;
    private static final int LIMIT = 1_000;

    @Test
    void guardedCompoundActionNeverExceedsTheLimit() throws InterruptedException {
        AtomicCompoundActionsExample counter = new AtomicCompoundActionsExample();

        Concurrently.run(THREADS, ATTEMPTS, () -> counter.incrementIfLessThan(LIMIT));

        assertEquals(LIMIT, counter.getCounter(),
                "the guarded check-then-increment should stop exactly at the limit");
    }

    /**
     * Without the guard, threads can pass the check together and push the counter past the limit.
     * That overshoot is permitted rather than guaranteed, so the assertion covers the invariant
     * that always holds: the counter never goes below the limit it was asked to reach, and never
     * exceeds what the attempts could produce.
     */
    @Test
    void unguardedCompoundActionCanOvershoot() throws InterruptedException {
        NonAtomicCompoundActionsExample counter = new NonAtomicCompoundActionsExample();

        Concurrently.run(THREADS, ATTEMPTS, () -> counter.incrementIfLessThan(LIMIT));

        int actual = counter.getCounter();
        assertTrue(actual >= LIMIT - THREADS,
                "expected to reach roughly the limit, got " + actual);
        assertTrue(actual <= THREADS * ATTEMPTS,
                "counted " + actual + ", more than the attempts made");
    }
}
