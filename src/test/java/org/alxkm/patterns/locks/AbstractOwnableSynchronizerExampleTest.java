package org.alxkm.patterns.locks;

import org.alxkm.patterns.locks.AbstractOwnableSynchronizerExample.OwnableSpinLock;
import org.alxkm.patterns.locks.AbstractOwnableSynchronizerExample.PlainSpinLock;
import org.alxkm.patterns.locks.AbstractOwnableSynchronizerExample.SpinLock;
import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class AbstractOwnableSynchronizerExampleTest {

    /**
     * The point of the example: the JVM lists the AOS-based lock as held, and not the other one.
     */
    @Test
    void onlyTheOwnableLockShowsUpInAThreadDump() throws InterruptedException {
        assumeTrue(ManagementFactory.getThreadMXBean().isSynchronizerUsageSupported(),
                "this JVM does not report ownable synchronizers");

        assertTrue(AbstractOwnableSynchronizerExample.visibleInThreadDump(new OwnableSpinLock()));
        assertFalse(AbstractOwnableSynchronizerExample.visibleInThreadDump(new PlainSpinLock()));
    }

    /**
     * Once released, the owner is cleared, so the dump stops listing it.
     */
    @Test
    void aReleasedLockIsNoLongerListed() {
        OwnableSpinLock lock = new OwnableSpinLock();
        lock.lock();
        assertTrue(AbstractOwnableSynchronizerExample.dumpShowsHolding(Thread.currentThread(), lock));

        lock.unlock();
        assertFalse(AbstractOwnableSynchronizerExample.dumpShowsHolding(Thread.currentThread(), lock));
    }

    @Test
    void bothLocksKeepEveryIncrementUnderContention() throws InterruptedException {
        for (SpinLock lock : List.of(new OwnableSpinLock(), new PlainSpinLock())) {
            int[] counter = {0};

            Concurrently.run(8, 10_000, () -> {
                lock.lock();
                try {
                    counter[0]++;
                } finally {
                    lock.unlock();
                }
            });

            assertEquals(80_000, counter[0], lock.getClass().getSimpleName());
        }
    }

    @Test
    void onlyTheOwnerCanUnlock() throws Exception {
        OwnableSpinLock lock = new OwnableSpinLock();
        lock.lock();

        try (ExecutorService other = Executors.newSingleThreadExecutor()) {
            ExecutionException thrown = assertThrows(ExecutionException.class,
                    () -> other.submit(lock::unlock).get());
            assertInstanceOf(IllegalMonitorStateException.class, thrown.getCause());
            assertFalse(other.submit(() -> lock.isHeldByCurrentThread()).get());
            assertTrue(lock.isHeldByCurrentThread());
        } finally {
            lock.unlock();
        }
    }

    @Test
    void itIsNotReentrant() {
        OwnableSpinLock lock = new OwnableSpinLock();
        lock.lock();
        try {
            assertFalse(lock.tryLock());
        } finally {
            lock.unlock();
        }
    }

    @Test
    void mainRuns() throws InterruptedException {
        AbstractOwnableSynchronizerExample.main(new String[0]);
    }
}
