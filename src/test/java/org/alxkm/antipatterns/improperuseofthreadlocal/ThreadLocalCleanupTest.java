package org.alxkm.antipatterns.improperuseofthreadlocal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the ThreadLocal cleanup antipattern and its two fixes.
 * <p>
 * The usual phrasing is that failing to call {@code remove} leaks memory, which is true but hard to
 * see and easy to dismiss. The consequence that actually bites arrives sooner: on a pooled thread the
 * value outlives the task that set it, so the next task to land on that thread inherits it. A request
 * handler that stored a user id this way serves the previous user's id to the next request.
 * <p>
 * A single-threaded executor makes that visible with no timing involved at all. Both tasks are
 * guaranteed to run on the same thread, so the second one sees exactly what the first one left behind.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ThreadLocalCleanupTest {

    @Test
    void valueLeaksIntoTheNextTaskOnTheSameThread() throws Exception {
        ThreadLocalExample example = new ThreadLocalExample();
        ExecutorService pool = Executors.newSingleThreadExecutor();

        try {
            pool.submit(() -> example.setThreadLocalValue("first task")).get();

            // Same thread, different task, and nothing was removed.
            String inherited = pool.submit(example::getThreadLocalValue).get();

            assertEquals("first task", inherited,
                    "without remove() the value outlives its task and the next one inherits it");
        } finally {
            shutdown(pool);
        }
    }

    @Test
    void removeInFinallyStopsTheLeak() throws Exception {
        ThreadLocalCleanupExample example = new ThreadLocalCleanupExample();
        ExecutorService pool = Executors.newSingleThreadExecutor();

        try {
            pool.submit(() -> {
                try {
                    example.setThreadLocalValue("first task");
                } finally {
                    example.removeThreadLocalValue();
                }
            }).get();

            assertNull(pool.submit(example::getThreadLocalValue).get(),
                    "remove() in a finally block must clear the value for the next task");
        } finally {
            shutdown(pool);
        }
    }

    /**
     * The same guarantee expressed with try-with-resources, which is harder to forget than a finally
     * block someone has to remember to write.
     */
    // The cleaner is deliberately unused inside the try body: it is held only so its close() runs the
    // remove() on the way out, which is exactly what -Xlint:try warns about.
    @SuppressWarnings("try")
    @Test
    void autoCloseableCleanerStopsTheLeak() throws Exception {
        ThreadLocalWithResourceExample example = new ThreadLocalWithResourceExample();
        ExecutorService pool = Executors.newSingleThreadExecutor();

        try {
            pool.submit(() -> {
                try (ThreadLocalWithResourceExample.ThreadLocalCleaner cleaner =
                             new ThreadLocalWithResourceExample.ThreadLocalCleaner()) {
                    example.setThreadLocalValue("first task");
                    assertEquals("first task", example.getThreadLocalValue());
                }
            }).get();

            assertNull(pool.submit(example::getThreadLocalValue).get(),
                    "the cleaner's close() must clear the value on the way out");
        } finally {
            shutdown(pool);
        }
    }

    /**
     * The part that is genuinely per-thread still works: two threads never see each other's value.
     * The leak is about reuse over time, not about isolation between threads.
     */
    @Test
    void valuesRemainPrivateToEachThread() throws Exception {
        ThreadLocalExample example = new ThreadLocalExample();
        ExecutorService first = Executors.newSingleThreadExecutor();
        ExecutorService second = Executors.newSingleThreadExecutor();

        try {
            first.submit(() -> example.setThreadLocalValue("owned by first")).get();
            second.submit(() -> example.setThreadLocalValue("owned by second")).get();

            assertEquals("owned by first", first.submit(example::getThreadLocalValue).get());
            assertEquals("owned by second", second.submit(example::getThreadLocalValue).get());
        } finally {
            shutdown(first);
            shutdown(second);
        }
    }

    /**
     * Shuts a pool down and waits for it, so a leftover worker cannot carry a value into another test.
     *
     * @param pool the pool to stop.
     * @throws InterruptedException if this thread is interrupted while waiting.
     * @throws ExecutionException   never, but keeps the calling signatures uniform.
     */
    private static void shutdown(ExecutorService pool) throws InterruptedException, ExecutionException {
        pool.shutdownNow();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("pool did not terminate");
        }
    }
}
