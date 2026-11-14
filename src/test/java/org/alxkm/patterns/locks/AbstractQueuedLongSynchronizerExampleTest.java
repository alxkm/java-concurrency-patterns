package org.alxkm.patterns.locks;

import org.alxkm.patterns.locks.AbstractQueuedLongSynchronizerExample.ByteBudget;
import org.alxkm.testsupport.Await;
import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the byte budget. Waiters are ordered by watching the AQS queue, not by sleeping.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class AbstractQueuedLongSynchronizerExampleTest {
    private static final long GIB = 1L << 30;

    @Test
    void itHoldsMoreThanAnIntCanCount() {
        ByteBudget budget = new ByteBudget(8 * GIB);

        assertTrue(budget.tryAcquire(5 * GIB));
        assertEquals(3 * GIB, budget.available());
        assertFalse(budget.tryAcquire(4 * GIB), "4 GiB does not fit in the 3 GiB left");
        assertEquals(3 * GIB, budget.available(), "a failed attempt should take nothing");
    }

    @Test
    void aWaiterGetsInOnlyOnceEnoughIsFree() throws Exception {
        ByteBudget budget = new ByteBudget(100);
        budget.acquire(100);

        try (ExecutorService other = Executors.newSingleThreadExecutor()) {
            Future<?> waiter = other.submit(() -> {
                budget.acquire(60);
                return null;
            });
            Await.until("the waiter is queued", () -> budget.getQueueLength() == 1);

            budget.release(30);
            // 30 free and 60 wanted: it cannot have got in, however the threads were scheduled.
            assertFalse(waiter.isDone(), "60 bytes should not fit in 30");

            budget.release(30);
            waiter.get(10, TimeUnit.SECONDS);
        }
        assertEquals(0, budget.available());
    }

    /**
     * Shared mode: one release can let in more than one waiter. An exclusive lock wakes one thread.
     */
    @Test
    void oneReleaseCanLetSeveralWaitersIn() throws Exception {
        ByteBudget budget = new ByteBudget(100);
        budget.acquire(100);

        try (ExecutorService others = Executors.newFixedThreadPool(2)) {
            Future<?> first = others.submit(() -> {
                budget.acquire(40);
                return null;
            });
            Future<?> second = others.submit(() -> {
                budget.acquire(40);
                return null;
            });
            Await.until("both waiters are queued", () -> budget.getQueueLength() == 2);

            budget.release(100);
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        assertEquals(20, budget.available());
    }

    @Test
    void theBudgetIsNeverOverspentUnderContention() throws InterruptedException {
        long capacity = 20;
        long request = 7;
        ByteBudget budget = new ByteBudget(capacity);
        AtomicLong inUse = new AtomicLong();
        AtomicLong mostInUse = new AtomicLong();

        Concurrently.run(8, 5_000, () -> {
            try {
                budget.acquire(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                mostInUse.accumulateAndGet(inUse.addAndGet(request), Math::max);
            } finally {
                inUse.addAndGet(-request);
                budget.release(request);
            }
        });

        assertTrue(mostInUse.get() <= capacity, "in use at once: " + mostInUse.get() + " of " + capacity);
        assertEquals(capacity, budget.available(), "every byte taken should have come back");
    }

    @Test
    void aRequestThatCouldNeverFitIsRejected() {
        ByteBudget budget = new ByteBudget(100);

        assertThrows(IllegalArgumentException.class, () -> budget.acquire(101));
        assertThrows(IllegalArgumentException.class, () -> budget.tryAcquire(0));
        assertThrows(IllegalArgumentException.class, () -> new ByteBudget(0));
    }

    @Test
    void mainRuns() throws InterruptedException {
        AbstractQueuedLongSynchronizerExample.main(new String[0]);
    }
}
