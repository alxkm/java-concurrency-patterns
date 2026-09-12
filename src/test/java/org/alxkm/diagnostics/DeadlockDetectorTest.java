package org.alxkm.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DeadlockDetector}.
 * <p>
 * Deadlocks created here are permanent: a thread blocked entering a monitor cannot be interrupted out
 * of it. So every test scopes its assertions to the threads it started, which is the behaviour
 * {@link DeadlockDetector#deadlockedAmong(Thread...)} exists to provide. Asserting on the JVM-wide view
 * would make these tests fail because of each other.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class DeadlockDetectorTest {

    @Test
    void detectsACycleOfTwoThreadsOnIntrinsicMonitors() throws InterruptedException {
        Deadlock deadlock = Deadlock.onMonitors();

        long[] found = awaitDeadlock(deadlock.first(), deadlock.second());

        assertNotNull(found, "the two threads hold each other's monitors, so this is a cycle");
        assertEquals(2, found.length);
    }

    /**
     * ReentrantLock is an ownable synchronizer rather than a monitor, and the JVM tracks those too. A
     * detector that only understood {@code synchronized} would miss this entirely.
     */
    @Test
    void detectsACycleOnReentrantLocks() throws InterruptedException {
        Deadlock deadlock = Deadlock.onLocks();

        long[] found = awaitDeadlock(deadlock.first(), deadlock.second());

        assertNotNull(found, "findDeadlockedThreads covers ownable synchronizers, not just monitors");
        assertEquals(2, found.length);
    }

    @Test
    void reportsNothingForThreadsThatAreNotDeadlocked() throws InterruptedException {
        Thread idle = new Thread(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "idle");
        idle.setDaemon(true);
        idle.start();

        try {
            assertNull(DeadlockDetector.deadlockedAmong(idle),
                    "a sleeping thread is not part of any cycle");
        } finally {
            idle.interrupt();
        }
    }

    /**
     * The report has to name both the lock a thread wants and the thread holding it, because that pair
     * is what identifies the two call sites taking locks in opposite orders.
     */
    @Test
    void describesBothSidesOfTheCycle() throws InterruptedException {
        Deadlock deadlock = Deadlock.onMonitors();
        awaitDeadlock(deadlock.first(), deadlock.second());

        String report = DeadlockDetector.describeDeadlocks();

        assertTrue(report.contains("Found a Java-level deadlock"), report);
        assertTrue(report.contains(deadlock.first().getName()), report);
        assertTrue(report.contains(deadlock.second().getName()), report);
        assertTrue(report.contains("waiting to lock"), report);
        assertTrue(report.contains("which is held by"), report);
    }

    /**
     * Polls until the JVM registers the cycle. The deadlock itself is guaranteed by the latch inside
     * {@link Deadlock}, so this only waits for the bookkeeping to catch up.
     *
     * @param threads the threads expected to deadlock.
     * @return the deadlocked ids.
     * @throws InterruptedException if this thread is interrupted while polling.
     */
    private static long[] awaitDeadlock(Thread... threads) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            long[] found = DeadlockDetector.deadlockedAmong(threads);
            if (found != null) {
                return found;
            }
            Thread.sleep(25);
        }
        return DeadlockDetector.deadlockedAmong(threads);
    }

    /** A pair of threads deadlocked on purpose, on either monitors or ReentrantLocks. */
    private record Deadlock(Thread first, Thread second) {

        static Deadlock onMonitors() throws InterruptedException {
            Object a = new Object();
            Object b = new Object();
            CountDownLatch bothHoldOne = new CountDownLatch(2);
            Thread one = start("monitor-deadlock-1", () -> {
                synchronized (a) {
                    countDownAndWait(bothHoldOne);
                    synchronized (b) {
                        throw new IllegalStateException("unreachable");
                    }
                }
            });
            Thread two = start("monitor-deadlock-2", () -> {
                synchronized (b) {
                    countDownAndWait(bothHoldOne);
                    synchronized (a) {
                        throw new IllegalStateException("unreachable");
                    }
                }
            });
            bothHoldOne.await();
            return new Deadlock(one, two);
        }

        static Deadlock onLocks() throws InterruptedException {
            ReentrantLock a = new ReentrantLock();
            ReentrantLock b = new ReentrantLock();
            CountDownLatch bothHoldOne = new CountDownLatch(2);
            Thread one = start("lock-deadlock-1", () -> {
                a.lock();
                countDownAndWait(bothHoldOne);
                b.lock();
            });
            Thread two = start("lock-deadlock-2", () -> {
                b.lock();
                countDownAndWait(bothHoldOne);
                a.lock();
            });
            bothHoldOne.await();
            return new Deadlock(one, two);
        }

        /**
         * Both threads must hold their first lock before either reaches for the second, or one simply
         * takes both and no cycle forms.
         *
         * @param latch the latch both threads count down and then wait on.
         */
        private static void countDownAndWait(CountDownLatch latch) {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private static Thread start(String name, Runnable body) {
            Thread thread = new Thread(body, name);
            thread.setDaemon(true);
            thread.start();
            return thread;
        }
    }
}
