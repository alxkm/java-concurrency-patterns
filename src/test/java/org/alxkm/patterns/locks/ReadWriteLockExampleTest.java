package org.alxkm.patterns.locks;

import org.alxkm.patterns.locks.ReadWriteLockExample.CachedValue;
import org.alxkm.testsupport.Await;
import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the read-write lock rules and the cache built on them. Orderings are set up by watching the
 * lock's queue, not by sleeping: a thread is known to wait for the lock once hasQueuedThread says so.
 * Other threads run on daemon executors that are shut down with shutdownNow(), so a thread a bug leaves
 * blocked fails the test instead of hanging the build.
 * <p>
 * The timeout runs each test on a separate thread. The default mode interrupts the test thread, and
 * lock() ignores interrupts: a read() that tries to upgrade would wait forever and take the build with it.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ReadWriteLockExampleTest {

    @Test
    void theOnlyReaderStillCannotUpgrade() {
        assertFalse(ReadWriteLockExample.tryUpgrade());
    }

    @Test
    void theWriterCanDowngrade() {
        assertTrue(ReadWriteLockExample.tryDowngrade());
    }

    @Test
    void loadsOnceAndAgainAfterInvalidate() {
        AtomicInteger loads = new AtomicInteger();
        CachedValue<String> cache = new CachedValue<>(() -> "v" + loads.incrementAndGet());

        assertEquals("v1", cache.read(v -> v));
        assertEquals("v1", cache.read(v -> v));
        cache.invalidate();
        assertEquals("v2", cache.read(v -> v));
        assertEquals(2, loads.get());
    }

    /**
     * Every reader waits inside read() until all of them are inside. With an exclusive lock only one
     * could ever be there, and the latch would time out.
     * <p>
     * The value is loaded first. On a cold cache a second reader may find it missing and queue for the
     * write lock; the readers after it then queue behind that writer, while the first reader waits
     * inside for them all. Nobody moves. Waiting inside a lock for other threads to arrive is a deadlock
     * waiting to happen, and here it is only done to prove the readers overlap.
     */
    @Test
    void readersUseTheValueAtTheSameTime() throws Exception {
        int readers = 4;
        CachedValue<String> cache = new CachedValue<>(() -> "value");
        cache.read(v -> v);
        CountDownLatch allInside = new CountDownLatch(readers);

        ExecutorService pool = daemonPool(readers);
        try {
            List<Future<Boolean>> results = pool.invokeAll(Collections.nCopies(readers, () ->
                    cache.read(v -> {
                        allInside.countDown();
                        try {
                            return allInside.await(10, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    })));
            for (Future<Boolean> result : results) {
                assertTrue(result.get(), "every reader was inside at the same time");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * While a reader is using the value, invalidate() waits. Then a new reader comes along. The read lock
     * is held, so it could get in, but it queues behind the waiting writer instead; otherwise a steady
     * stream of readers would keep the writer out forever. When the first reader is done, the writer goes
     * first and the new reader sees a fresh load.
     */
    @Test
    void aWriterWaitsForTheReaderAndANewReaderWaitsForTheWriter() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        CachedValue<String> cache = new CachedValue<>(() -> "v" + loads.incrementAndGet());
        ReentrantReadWriteLock lock = cache.lock();
        CountDownLatch firstReaderInside = new CountDownLatch(1);
        CountDownLatch firstReaderMayLeave = new CountDownLatch(1);
        AtomicReference<Thread> writerThread = new AtomicReference<>();
        AtomicReference<Thread> secondReaderThread = new AtomicReference<>();

        ExecutorService pool = daemonPool(3);
        try {
            Future<String> firstReader = pool.submit(() -> cache.read(v -> {
                firstReaderInside.countDown();
                await(firstReaderMayLeave);
                return v;
            }));
            assertTrue(firstReaderInside.await(10, TimeUnit.SECONDS));

            Future<?> writer = pool.submit(() -> {
                writerThread.set(Thread.currentThread());
                cache.invalidate();
            });
            Await.until("invalidate() waits for the lock",
                    () -> writerThread.get() != null && lock.hasQueuedThread(writerThread.get()));

            Future<String> secondReader = pool.submit(() -> {
                secondReaderThread.set(Thread.currentThread());
                return cache.read(v -> v);
            });
            Await.until("the new reader waits behind the writer",
                    () -> secondReaderThread.get() != null && lock.hasQueuedThread(secondReaderThread.get()));
            assertFalse(writer.isDone());
            assertFalse(secondReader.isDone());

            firstReaderMayLeave.countDown();
            assertEquals("v1", firstReader.get(10, TimeUnit.SECONDS));
            writer.get(10, TimeUnit.SECONDS);
            assertEquals("v2", secondReader.get(10, TimeUnit.SECONDS), "the writer went first");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Why read() downgrades instead of letting go of the write lock and then taking the read lock. The
     * loader runs under the write lock, and while it does, invalidate() starts and queues for the lock.
     * With the downgrade, the read lock is already held when the write lock is let go, so invalidate()
     * keeps waiting and the value reaches the caller. Without it, invalidate() gets in through the gap,
     * and the caller is handed null.
     */
    @Test
    void aWriterQueuedDuringTheLoadCannotGetInBeforeTheValueIsUsed() throws Exception {
        AtomicReference<Thread> writerThread = new AtomicReference<>();
        ExecutorService pool = daemonPool(1);
        try {
            AtomicReference<CachedValue<String>> holder = new AtomicReference<>();
            AtomicReference<Future<?>> writer = new AtomicReference<>();
            CachedValue<String> cache = new CachedValue<>(() -> {
                writer.set(pool.submit(() -> {
                    writerThread.set(Thread.currentThread());
                    holder.get().invalidate();
                }));
                awaitQuietly(() -> writerThread.get() != null
                        && holder.get().lock().hasQueuedThread(writerThread.get()));
                return "loaded";
            });
            holder.set(cache);

            assertEquals("loaded", cache.read(v -> v));
            writer.get().get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * After every invalidate, many threads read at once. Exactly one of them may load: the others either
     * find the value loaded, or wait for the write lock and then must check again before loading.
     */
    @Test
    void oneLoadPerInvalidateUnderContention() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        CachedValue<Integer> cache = new CachedValue<>(() -> {
            int load = loads.incrementAndGet();
            for (int i = 0; i < 1_000; i++) {
                Thread.onSpinWait(); // a load takes a moment, so readers pile up behind it
            }
            return load;
        });

        for (int round = 1; round <= 300; round++) {
            cache.invalidate();
            Concurrently.run(8, 1, () -> cache.read(v -> v));
            assertEquals(round, loads.get(), "loads after round " + round);
        }
    }

    @Test
    void aFailingLoadLeavesTheLockFreeAndTheNextReadRetries() {
        AtomicInteger calls = new AtomicInteger();
        CachedValue<String> cache = new CachedValue<>(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("backend down");
            }
            return "value";
        });

        assertThrows(IllegalStateException.class, () -> cache.read(v -> v));
        ReentrantReadWriteLock lock = cache.lock();
        assertFalse(lock.isWriteLocked());
        assertEquals(0, lock.getReadLockCount());

        assertEquals("value", cache.read(v -> v));
        assertEquals(0, lock.getReadLockCount());
    }

    @Test
    void readReturnsWhatUseReturns() {
        CachedValue<String> cache = new CachedValue<>(() -> "abc");
        assertEquals(3, cache.read(String::length));
        Object marker = new Object();
        assertSame(marker, cache.read(v -> marker));
    }

    @Test
    void mainRuns() {
        ReadWriteLockExample.main(new String[0]);
    }

    private static ExecutorService daemonPool(int threads) {
        return Executors.newFixedThreadPool(threads, task -> {
            Thread thread = new Thread(task, "rw-test");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch not released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void awaitQuietly(BooleanSupplier condition) {
        try {
            Await.until("the writer is queued", condition);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
