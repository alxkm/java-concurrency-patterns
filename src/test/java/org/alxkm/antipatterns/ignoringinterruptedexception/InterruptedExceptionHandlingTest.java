package org.alxkm.antipatterns.ignoringinterruptedexception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for swallowing InterruptedException, and the two correct responses.
 * <p>
 * Interruption in Java is a request, not a command. Nothing stops a thread from the outside: the only
 * thing an interrupt does is set a flag and make blocking calls throw. A thread that catches
 * InterruptedException and carries on has declined the request, and no amount of calling interrupt()
 * will change its mind. That is what these tests demonstrate.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class InterruptedExceptionHandlingTest {

    /** The examples sleep in one second steps, so give them a couple of cycles to react. */
    private static final long REACTION_MILLIS = 2_500;

    /**
     * The antipattern: interrupt is ignored, so the thread keeps running.
     * <p>
     * The thread is left running deliberately. There is no way to stop it, which is the entire point,
     * so it is a daemon and the JVM can exit regardless.
     */
    @Test
    void swallowedInterruptLeavesTheThreadRunning() throws InterruptedException {
        Thread worker = new Thread(new IgnoringInterruptedException(), "ignores-interrupts");
        worker.setDaemon(true);
        worker.start();

        worker.interrupt();
        worker.join(REACTION_MILLIS);

        assertTrue(worker.isAlive(),
                "the catch block discards the interruption, so nothing can stop this thread");
    }

    /**
     * The fix for a Runnable: restore the flag and leave the loop.
     */
    @Test
    void restoringTheFlagAndBreakingStopsTheThread() throws InterruptedException {
        Thread worker = new Thread(new ProperlyHandlingInterruptedException(), "handles-interrupts");
        worker.setDaemon(true);
        worker.start();

        worker.interrupt();
        worker.join(REACTION_MILLIS);

        assertFalse(worker.isAlive(), "handling the interruption should end the loop");
        assertTrue(worker.getState() == Thread.State.TERMINATED,
                "expected TERMINATED but was " + worker.getState());
    }

    /**
     * The fix when the signature allows it: let the exception travel to the caller.
     * <p>
     * A Runnable cannot do this, since run() declares no checked exceptions. Implementing Callable is
     * what makes propagation available, and the executor then delivers the outcome through the Future.
     */
    @Test
    void propagatingLetsTheCallerSeeTheCancellation() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Void> future = executor.submit(new PropagatingInterruptedException());

            assertTrue(future.cancel(true), "cancel(true) should interrupt the running task");

            // The caller learns the task did not finish, rather than the fact being swallowed.
            assertThrows(CancellationException.class, future::get);
            assertTrue(future.isCancelled());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                    "a task that propagates interruption should let its pool shut down");
        }
    }

    /**
     * The pool difference the antipattern causes. shutdownNow interrupts the workers, so a task that
     * honours interruption lets the pool terminate and one that swallows it does not.
     */
    @Test
    void swallowingInterruptionKeepsAPoolFromShuttingDown()
            throws InterruptedException, ExecutionException {
        ExecutorService swallowing = Executors.newSingleThreadExecutor(daemonFactory());
        swallowing.submit(new IgnoringInterruptedException());
        swallowing.shutdownNow();
        assertFalse(swallowing.awaitTermination(REACTION_MILLIS, TimeUnit.MILLISECONDS),
                "a task that ignores interruption blocks shutdown indefinitely");

        ExecutorService handling = Executors.newSingleThreadExecutor(daemonFactory());
        handling.submit(new ProperlyHandlingInterruptedException());
        handling.shutdownNow();
        assertTrue(handling.awaitTermination(REACTION_MILLIS, TimeUnit.MILLISECONDS),
                "a task that honours interruption lets the pool terminate");
    }

    /**
     * Daemon workers, so the task that cannot be stopped does not keep the JVM alive after the suite.
     *
     * @return a thread factory producing daemon threads.
     */
    private static java.util.concurrent.ThreadFactory daemonFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        };
    }
}
