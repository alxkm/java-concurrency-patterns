package org.alxkm.patterns.locks;

import org.alxkm.patterns.locks.AbstractQueuedSynchronizerExample.Mutex;
import org.alxkm.testsupport.Await;
import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the AQS mutex. Whatever another thread does or sees is returned through a Future and
 * asserted here, on the test thread.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class AbstractQueuedSynchronizerExampleTest {

    @Test
    void noIncrementIsLostUnderContention() throws InterruptedException {
        Mutex mutex = new Mutex();
        int[] counter = {0};

        Concurrently.run(8, 10_000, () -> {
            mutex.lock();
            try {
                counter[0]++;
            } finally {
                mutex.unlock();
            }
        });

        assertEquals(80_000, counter[0]);
        assertFalse(mutex.isLocked());
    }

    /**
     * The bug this example used to have: tryRelease set the state to 0 for whoever called it.
     */
    @Test
    void onlyTheOwnerCanUnlock() throws Exception {
        Mutex mutex = new Mutex();
        mutex.lock();

        try (ExecutorService other = Executors.newSingleThreadExecutor()) {
            ExecutionException thrown = assertThrows(ExecutionException.class,
                    () -> other.submit(mutex::unlock).get());
            assertInstanceOf(IllegalMonitorStateException.class, thrown.getCause());

            assertTrue(mutex.isHeldByCurrentThread(), "the owner should still hold the lock");
            assertFalse(other.submit(() -> mutex.tryLock()).get(), "no other thread should get in");
        } finally {
            mutex.unlock();
        }
    }

    @Test
    void unlockingAFreeMutexThrows() {
        assertThrows(IllegalMonitorStateException.class, new Mutex()::unlock);
    }

    @Test
    void itIsNotReentrant() {
        Mutex mutex = new Mutex();
        mutex.lock();
        try {
            assertFalse(mutex.tryLock(), "the owner should not be able to take it a second time");
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Ordered by the queue, not by sleeping: the waiter is known to be parked once AQS has queued it.
     */
    @Test
    void aQueuedThreadGetsTheLockWhenItIsReleased() throws Exception {
        Mutex mutex = new Mutex();
        mutex.lock();

        try (ExecutorService other = Executors.newSingleThreadExecutor()) {
            Future<Boolean> waiter = other.submit(() -> {
                mutex.lock();
                try {
                    return mutex.isHeldByCurrentThread();
                } finally {
                    mutex.unlock();
                }
            });

            Await.until("the waiter is queued", () -> mutex.getQueueLength() == 1);
            assertFalse(waiter.isDone(), "the waiter should not get in while the lock is held");

            mutex.unlock();
            assertTrue(waiter.get(10, TimeUnit.SECONDS));
        }
        assertFalse(mutex.isLocked());
    }

    @Test
    void timedTryLockGivesUpWhileAnotherThreadHoldsIt() throws Exception {
        Mutex mutex = new Mutex();
        mutex.lock();

        try (ExecutorService other = Executors.newSingleThreadExecutor()) {
            assertFalse(other.submit(() -> mutex.tryLock(50, TimeUnit.MILLISECONDS)).get());
        } finally {
            mutex.unlock();
        }
    }

    @Test
    void mainRuns() throws InterruptedException {
        AbstractQueuedSynchronizerExample.main(new String[0]);
    }
}
