package org.alxkm.patterns.reentrantlock;

import org.alxkm.patterns.reentrantlock.ReentrantLockExample.Account;
import org.alxkm.testsupport.Await;
import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the transfer that backs off and for interruptible locking. The dangerous interleaving, both
 * transfers holding one lock each, is forced with a barrier rather than hoped for.
 * <p>
 * The timeout runs each test on a separate thread: lock() ignores interrupts, so a transfer that waited
 * for its second lock would otherwise hang the build instead of failing the test.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ReentrantLockExampleTest {

    private final ExecutorService executor = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "lock-test");
        thread.setDaemon(true);
        return thread;
    });

    @AfterEach
    void shutDown() {
        executor.shutdownNow();
    }

    /**
     * The failure itself, reported by the JVM. If this ever returns an empty list, the example no longer
     * shows the deadlock.
     */
    @Test
    void oppositeTransfersWithBlockingLocksDeadlock() throws InterruptedException {
        assertEquals(List.of("A-to-B", "B-to-A"), ReentrantLockExample.deadlockedOppositeTransfers());
    }

    /**
     * The same interleaving with back-off: both transfers hold their first lock when they try the second.
     * Both fail, let go, and retry, and both go through.
     */
    @Test
    void oppositeTransfersThatBackOffBothGoThrough() throws Exception {
        Account a = new Account("A", 100);
        Account b = new Account("B", 100);
        CyclicBarrier bothHoldOneLock = new CyclicBarrier(2);
        AtomicInteger attemptsPastTheBarrier = new AtomicInteger();

        Future<Boolean> aToB = executor.submit(() -> ReentrantLockExample.transfer(a, b, 30, 10, TimeUnit.SECONDS,
                () -> meet(bothHoldOneLock, attemptsPastTheBarrier)));
        Future<Boolean> bToA = executor.submit(() -> ReentrantLockExample.transfer(b, a, 10, 10, TimeUnit.SECONDS,
                () -> meet(bothHoldOneLock, attemptsPastTheBarrier)));

        assertTrue(aToB.get(20, TimeUnit.SECONDS));
        assertTrue(bToA.get(20, TimeUnit.SECONDS));
        assertEquals(2, attemptsPastTheBarrier.get(), "both held one lock at the same time");
        assertEquals(80, a.balance());
        assertEquals(120, b.balance());
    }

    /**
     * Real threads, both directions, no forcing. Every transfer must go through and no money may
     * appear or vanish.
     */
    @Test
    void manyOppositeTransfersKeepTheTotal() throws InterruptedException {
        // Enough on each side for the worst order: every transfer one way before any the other way.
        Account a = new Account("A", 10_000);
        Account b = new Account("B", 10_000);
        AtomicInteger next = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        Concurrently.run(4, 1, () -> {
            boolean forward = next.getAndIncrement() % 2 == 0;
            for (int i = 0; i < 5_000; i++) {
                try {
                    boolean done = forward
                            ? ReentrantLockExample.transfer(a, b, 1, 10, TimeUnit.SECONDS)
                            : ReentrantLockExample.transfer(b, a, 1, 10, TimeUnit.SECONDS);
                    if (!done) {
                        failed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        assertEquals(0, failed.get(), "transfers that gave up");
        assertEquals(10_000, a.balance(), "as many transfers each way, so both end where they began");
        assertEquals(10_000, b.balance());
    }

    @Test
    void aTransferGivesUpAtTheDeadline() throws Exception {
        Account a = new Account("A", 100);
        Account b = new Account("B", 100);
        b.lock().lock(); // held by this thread for the whole test
        try {
            Future<Boolean> transfer = executor.submit(() -> ReentrantLockExample.transfer(a, b, 10, 50, TimeUnit.MILLISECONDS));

            assertFalse(transfer.get(10, TimeUnit.SECONDS));
            assertFalse(a.lock().isLocked(), "the first lock was let go");
        } finally {
            b.lock().unlock();
        }
        assertEquals(100, a.balance());
        assertEquals(100, b.balance());
    }

    @Test
    void aTransferBackingOffCanBeInterrupted() throws Exception {
        Account a = new Account("A", 100);
        Account b = new Account("B", 100);
        AtomicReference<Thread> transferThread = new AtomicReference<>();
        b.lock().lock();
        try {
            Future<Boolean> transfer = executor.submit(() -> {
                transferThread.set(Thread.currentThread());
                return ReentrantLockExample.transfer(a, b, 10, 30, TimeUnit.SECONDS);
            });
            Await.until("the transfer is running", () -> transferThread.get() != null);

            transferThread.get().interrupt();

            ExecutionException thrown = assertThrows(ExecutionException.class, () -> transfer.get(10, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, thrown.getCause());
            assertFalse(a.lock().isLocked());
        } finally {
            b.lock().unlock();
        }
    }

    @Test
    void aTransferWithoutTheMoneyFailsAndLetsGoOfBothLocks() {
        Account a = new Account("A", 5);
        Account b = new Account("B", 0);

        assertThrows(IllegalStateException.class, () -> ReentrantLockExample.transfer(a, b, 10, 1, TimeUnit.SECONDS));
        assertFalse(a.lock().isLocked());
        assertFalse(b.lock().isLocked());
        assertEquals(5, a.balance());
    }

    /**
     * Both readers wait for a lock this thread holds, and both are interrupted. lockInterruptibly() gives
     * up at once. lock() goes on waiting; it notes the interrupt and sets the flag again only once it has
     * the lock, so the interrupt is late but not lost.
     */
    @Test
    void onlyLockInterruptiblyStopsWaitingWhenInterrupted() throws Exception {
        Account account = new Account("A", 42);
        AtomicReference<Thread> interruptibleReader = new AtomicReference<>();
        AtomicReference<Thread> plainReader = new AtomicReference<>();
        Future<Boolean> plain;
        account.lock().lock();
        try {
            Future<Long> interruptible = executor.submit(() -> {
                interruptibleReader.set(Thread.currentThread());
                return account.balanceInterruptibly();
            });
            plain = executor.submit(() -> {
                plainReader.set(Thread.currentThread());
                account.balance();
                return Thread.currentThread().isInterrupted();
            });
            Await.until("both readers wait for the lock", () -> interruptibleReader.get() != null
                    && plainReader.get() != null
                    && account.lock().hasQueuedThread(interruptibleReader.get())
                    && account.lock().hasQueuedThread(plainReader.get()));

            interruptibleReader.get().interrupt();
            plainReader.get().interrupt();

            ExecutionException thrown = assertThrows(ExecutionException.class,
                    () -> interruptible.get(10, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, thrown.getCause());
            assertTrue(account.lock().hasQueuedThread(plainReader.get()), "lock() is still waiting");
            assertFalse(plain.isDone());
        } finally {
            account.lock().unlock();
        }
        assertTrue(plain.get(10, TimeUnit.SECONDS), "lock() got the lock, and the interrupt flag is set again");
    }

    @Test
    void mainRuns() throws InterruptedException {
        ReentrantLockExample.main(new String[0]);
    }

    private static void meet(CyclicBarrier barrier, AtomicInteger passed) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
            passed.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }
}
