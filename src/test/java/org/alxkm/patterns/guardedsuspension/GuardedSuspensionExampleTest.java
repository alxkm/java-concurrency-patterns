package org.alxkm.patterns.guardedsuspension;

import org.alxkm.testsupport.Await;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

class GuardedSuspensionExampleTest {

    /**
     * This test method verifies the behavior of the GuardedSuspensionExample class, which demonstrates
     * the Guarded Suspension pattern for coordinating communication between threads. It creates two threads,
     * t1 and t2, where t1 waits for a condition to be signaled by t2. The test ensures that t1 is waiting
     * for the condition by waiting until it actually reaches Thread.State.WAITING before starting t2,
     * which is the condition a fixed sleep could only guess at. After both threads have completed,
     * the test checks that neither is alive, confirming the signal released the waiter.
     */
    @Test
    void testGuardedSuspension() throws InterruptedException {
        GuardedSuspensionExample example = new GuardedSuspensionExample();
        Thread t1 = new Thread(example::awaitCondition);
        Thread t2 = new Thread(example::signalCondition);

        t1.start();
        // Block until t1 has genuinely parked in wait(), so the signal cannot be delivered early.
        Await.until("t1 to be waiting on the condition", () -> t1.getState() == Thread.State.WAITING);
        t2.start();

        t1.join(TimeUnit.SECONDS.toMillis(10));
        t2.join(TimeUnit.SECONDS.toMillis(10));

        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
    }
}

