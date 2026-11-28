package org.alxkm.testsupport;

import org.alxkm.testsupport.UncaughtExceptions.Death;
import org.alxkm.testsupport.UncaughtExceptions.Recorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the recorder behind {@link UncaughtExceptions}. Each test installs a recorder of its own,
 * which keeps the deaths it records from the one the extension installed around the test, so the
 * threads that die here on purpose do not fail the test.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class UncaughtExceptionsTest {

    @Test
    void recordsAThreadThatDies() throws InterruptedException {
        IllegalStateException error = new IllegalStateException("on purpose");
        Recorder recorder = Recorder.install();
        List<Death> deaths;
        try {
            Thread thread = new Thread(() -> {
                throw error;
            }, "dies-on-purpose");
            thread.start();
            thread.join();
        } finally {
            deaths = recorder.uninstall();
        }

        assertEquals(1, deaths.size());
        assertEquals("dies-on-purpose", deaths.get(0).thread());
        assertSame(error, deaths.get(0).error());
    }

    @Test
    void putsBackTheHandlerItReplaced() {
        Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();

        Recorder recorder = Recorder.install();
        assertSame(recorder, Thread.getDefaultUncaughtExceptionHandler());
        recorder.uninstall();

        assertSame(before, Thread.getDefaultUncaughtExceptionHandler());
    }

    /**
     * execute() runs the task on the worker thread itself, so its exception kills the worker and is
     * recorded. submit() wraps the task in a FutureTask that keeps the exception for get(), and nothing
     * dies.
     */
    @Test
    void seesExecuteButNotSubmit() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "executor-worker");
            thread.setDaemon(true);
            return thread;
        });
        Recorder recorder = Recorder.install();
        List<Death> deaths;
        try {
            Future<?> submitted = executor.submit(() -> {
                throw new IllegalStateException("kept by the future");
            });
            assertThrows(ExecutionException.class, () -> submitted.get(10, TimeUnit.SECONDS));

            executor.execute(() -> {
                throw new IllegalStateException("kills the worker");
            });
            Await.until("the worker died", () -> !recorder.deaths().isEmpty());
        } finally {
            deaths = recorder.uninstall();
            executor.shutdownNow();
        }

        assertEquals(1, deaths.size(), "only the task passed to execute() killed its thread");
        assertEquals("kills the worker", deaths.get(0).error().getMessage());
        assertTrue(deaths.get(0).thread().startsWith("executor-worker"));
    }
}
