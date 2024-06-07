package org.alxkm.patterns.guardedsuspension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class GuardedSuspensionExampleTest {

    /**
     * This test method verifies the behavior of the GuardedSuspensionExample class, which demonstrates
     * the Guarded Suspension pattern for coordinating communication between threads. It creates two threads,
     * t1 and t2, where t1 waits for a condition to be signaled by t2. The test ensures that t1 is waiting
     * for the condition by introducing a delay before starting t2. After both threads have completed their
     * execution or timed out, the test checks that neither thread is alive, confirming that both threads
     * have finished their respective tasks as expected.
     */
    @Test
    public void testGuardedSuspension() throws InterruptedException {
        GuardedSuspensionExample example = new GuardedSuspensionExample();
        Thread t1 = new Thread(example::awaitCondition);
        Thread t2 = new Thread(example::signalCondition);

        t1.start();
        Thread.sleep(100); // Ensure t1 is waiting
        t2.start();

        t1.join(1000);
        t2.join(1000);

        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
    }
}

