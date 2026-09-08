package org.alxkm.testsupport;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Waits for a condition to hold instead of sleeping for a guessed interval.
 * <p>
 * A fixed {@code Thread.sleep} in a concurrency test is wrong in both directions: too short and the
 * test fails on a loaded CI runner, too long and every run pays for the worst case. Polling a real
 * condition up to a generous deadline finishes as soon as the condition holds, and reports a clear
 * failure rather than a mysterious assertion mismatch when it never does.
 */
public final class Await {
    private static final long DEFAULT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private static final long POLL_INTERVAL_MILLIS = 5;

    private Await() {
    }

    /**
     * Blocks until the condition holds, failing the test if it does not within the default timeout.
     *
     * @param description what is being waited for, used in the failure message.
     * @param condition   the condition to poll.
     * @throws InterruptedException if the waiting thread is interrupted.
     */
    public static void until(String description, BooleanSupplier condition) throws InterruptedException {
        until(description, condition, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Blocks until the condition holds, failing the test if it does not within the given timeout.
     *
     * @param description   what is being waited for, used in the failure message.
     * @param condition     the condition to poll.
     * @param timeoutMillis how long to keep polling before giving up.
     * @throws InterruptedException if the waiting thread is interrupted.
     */
    public static void until(String description, BooleanSupplier condition, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        if (!condition.getAsBoolean()) {
            fail("Timed out after " + timeoutMillis + "ms waiting for: " + description);
        }
    }
}
