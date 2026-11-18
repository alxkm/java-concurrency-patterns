package org.alxkm.patterns.locks;

import org.alxkm.patterns.locks.LockSupportExample.OneShotSignal;
import org.alxkm.testsupport.Await;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for LockSupport and the signal built on it.
 * <p>
 * A wait that should end but does not is caught by a timeout. The waiters run on daemon threads and
 * the executor is shut down with shutdownNow(), never close(): close() waits for the tasks to finish,
 * and a waiter that a bug leaves parked or spinning would hang the build instead of failing the test.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class LockSupportExampleTest {

    /**
     * Would block forever if the unpark before the park were lost.
     */
    @Test
    void anUnparkBeforeTheParkIsNotLost() {
        LockSupportExample.parkAfterUnpark();
    }

    @Test
    void aSignalBeforeTheWaitLetsTheWaiterThroughAtOnce() throws InterruptedException {
        OneShotSignal signal = new OneShotSignal();
        signal.signal();

        signal.await();
    }

    /**
     * The waiter is known to be parked once its blocker is the signal; only then is it signalled.
     */
    @Test
    void aParkedWaiterWakesWhenSignalled() throws Exception {
        OneShotSignal signal = new OneShotSignal();
        AtomicReference<Thread> waiterThread = new AtomicReference<>();

        ExecutorService other = daemonExecutor();
        try {
            Future<?> waiter = other.submit(() -> {
                waiterThread.set(Thread.currentThread());
                signal.await();
                return null;
            });
            Await.until("the waiter is parked on the signal",
                    () -> waiterThread.get() != null && LockSupport.getBlocker(waiterThread.get()) == signal);
            assertFalse(waiter.isDone());

            signal.signal();
            waiter.get(10, TimeUnit.SECONDS);
        } finally {
            other.shutdownNow();
        }
    }

    /**
     * Signal and await race from the start, many times over. Whichever side goes first, the waiter must
     * get through: either it sees the flag, or signal() sees it and unparks it.
     */
    @Test
    void noSignalIsLostWhenSignalAndAwaitRace() throws Exception {
        ExecutorService other = daemonExecutor();
        try {
            for (int round = 0; round < 10_000; round++) {
                OneShotSignal signal = new OneShotSignal();
                Future<?> waiter = other.submit(() -> {
                    signal.await();
                    return null;
                });
                signal.signal();
                waiter.get(10, TimeUnit.SECONDS);
            }
        } finally {
            other.shutdownNow();
        }
    }

    @Test
    void anInterruptedWaiterGetsInterruptedException() throws Exception {
        OneShotSignal signal = new OneShotSignal();
        AtomicReference<Thread> waiterThread = new AtomicReference<>();

        ExecutorService other = daemonExecutor();
        try {
            Future<?> waiter = other.submit(() -> {
                waiterThread.set(Thread.currentThread());
                signal.await();
                return null;
            });
            Await.until("the waiter is parked on the signal",
                    () -> waiterThread.get() != null && LockSupport.getBlocker(waiterThread.get()) == signal);

            waiterThread.get().interrupt();
            ExecutionException thrown = assertThrows(ExecutionException.class, () -> waiter.get(10, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, thrown.getCause());
        } finally {
            other.shutdownNow();
        }
    }

    private static ExecutorService daemonExecutor() {
        return Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "signal-waiter");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Test
    void mainRuns() throws InterruptedException {
        LockSupportExample.main(new String[0]);
    }
}
