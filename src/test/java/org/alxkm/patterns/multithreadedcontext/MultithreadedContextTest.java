package org.alxkm.patterns.multithreadedcontext;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MultithreadedContext}.
 * <p>
 * The class hands out one lock behind a singleton and runs arbitrary work under it. Three things are
 * worth pinning down: that the mutual exclusion is real, that {@code tryApply} declines rather than
 * waits, and that the lock is released when the work throws. The last one is where this shape of
 * helper usually breaks, because a missing finally leaves the lock held forever and every later
 * caller hangs on a lock nobody owns any more.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class MultithreadedContextTest {

    @Test
    void getInstanceAlwaysReturnsTheSameContext() throws Exception {
        List<MultithreadedContext> seen = Concurrently.collect(8, MultithreadedContext::getInstance);

        assertEquals(8, seen.size());
        seen.forEach(instance -> assertSame(MultithreadedContext.getInstance(), instance));
    }

    /**
     * The point of the class: increments run under the lock, so none is lost. A plain counter in the
     * same loop would come out short.
     */
    @Test
    void applySerialisesConcurrentWork() throws Exception {
        MultithreadedContext context = MultithreadedContext.getInstance();
        AtomicInteger counter = new AtomicInteger();
        int threads = 8;
        int iterations = 2_000;

        Concurrently.run(threads, iterations, () -> context.apply(counter::incrementAndGet));

        assertEquals(threads * iterations, counter.get());
    }

    /**
     * tryApply declines when the lock is taken rather than queueing behind it. That is the whole
     * difference from apply, and the reason to reach for it: the caller keeps control instead of
     * blocking for an unknown length of time.
     */
    @Test
    void tryApplyDeclinesWhileTheLockIsHeld() throws Exception {
        MultithreadedContext context = MultithreadedContext.getInstance();
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger ranWhileHeld = new AtomicInteger();

        Thread holder = new Thread(() -> context.apply(() -> {
            holding.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }), "lock-holder");
        holder.setDaemon(true);
        holder.start();

        assertTrue(holding.await(10, TimeUnit.SECONDS));
        try {
            assertFalse(context.tryApply(ranWhileHeld::incrementAndGet),
                    "the lock is held, so tryApply should decline");
            assertEquals(0, ranWhileHeld.get(), "the declined action must not have run");
        } finally {
            release.countDown();
            holder.join(TimeUnit.SECONDS.toMillis(10));
        }

        assertTrue(context.tryApply(ranWhileHeld::incrementAndGet),
                "the lock is free again, so tryApply should succeed");
        assertEquals(1, ranWhileHeld.get());
    }

    /**
     * A throwing action must still release the lock. Without the finally this passes once and then
     * every later caller hangs on a lock whose owner is long gone.
     */
    @Test
    void lockIsReleasedWhenTheActionThrows() {
        MultithreadedContext context = MultithreadedContext.getInstance();

        try {
            context.apply(() -> {
                throw new IllegalStateException("action failed on purpose");
            });
        } catch (IllegalStateException expected) {
            // propagated to the caller, which is correct
        }

        assertTrue(context.tryApply(() -> { }),
                "the lock should have been released despite the exception");
    }
}
