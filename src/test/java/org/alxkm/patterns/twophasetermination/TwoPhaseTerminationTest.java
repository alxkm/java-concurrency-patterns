package org.alxkm.patterns.twophasetermination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TwoPhaseTerminationTest {

    /**
     * This test method verifies the behavior of the Two-Phase Termination pattern implementation.
     * It ensures that a thread can be gracefully terminated using the Two-Phase Termination pattern.
     */
    @Test
    public void testTwoPhaseTermination() throws InterruptedException {
        TwoPhaseTermination thread = new TwoPhaseTermination();
        thread.start();

        Thread.sleep(500); // Let the thread run for a while
        thread.terminate();
        thread.join();

        assertFalse(thread.isAlive());
    }
}

