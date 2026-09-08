package org.alxkm.patterns.twophasetermination;

import org.alxkm.testsupport.Await;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TwoPhaseTerminationTest {

    /**
     * This test method verifies the behavior of the Two-Phase Termination pattern implementation.
     * It ensures that a thread can be gracefully terminated using the Two-Phase Termination pattern.
     */
    @Test
    void testTwoPhaseTermination() throws InterruptedException {
        TwoPhaseTermination thread = new TwoPhaseTermination();
        thread.start();

        // Wait until the worker is demonstrably inside its work loop. Sleeping a fixed interval
        // would either race the thread's startup or pad every run with the worst case.
        Await.until("the worker to enter its work loop",
                () -> thread.getState() == Thread.State.TIMED_WAITING);

        thread.terminate();
        thread.join(TimeUnit.SECONDS.toMillis(10));

        assertFalse(thread.isAlive(), "terminate() should have stopped the thread");
    }
}

