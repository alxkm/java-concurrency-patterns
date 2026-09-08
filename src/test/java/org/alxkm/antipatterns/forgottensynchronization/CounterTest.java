package org.alxkm.antipatterns.forgottensynchronization;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.IntSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrasts the unsynchronized counter with the two resolutions offered alongside it.
 */
class CounterTest {
    private static final int THREADS = 4;
    private static final int INCREMENTS = 50_000;
    private static final int EXPECTED = THREADS * INCREMENTS;

    private static Stream<Arguments> synchronizedCounters() {
        CounterSynchronized intrinsic = new CounterSynchronized();
        CounterReentrantLockResolution explicit = new CounterReentrantLockResolution();
        return Stream.of(
                Arguments.of("synchronized methods", (Runnable) intrinsic::increment,
                        (IntSupplier) intrinsic::getCounter),
                Arguments.of("ReentrantLock", (Runnable) explicit::increment,
                        (IntSupplier) explicit::getCount));
    }

    @ParameterizedTest(name = "{0} loses no increments")
    @MethodSource("synchronizedCounters")
    void guardedCountersLoseNoIncrements(String label, Runnable increment, IntSupplier read)
            throws InterruptedException {
        Concurrently.run(THREADS, INCREMENTS, increment);

        assertEquals(EXPECTED, read.getAsInt(), label + " lost increments");
    }

    /**
     * The unguarded counter can only lose increments, never manufacture them. Whether a given run
     * actually loses any is up to the JVM, so the assertion is on the invariant that always holds.
     */
    @Test
    void unsynchronizedCounterNeverOvercounts() throws InterruptedException {
        CounterExample counter = new CounterExample();

        Concurrently.run(THREADS, INCREMENTS, counter::increment);

        int actual = counter.getCounter();
        assertTrue(actual <= EXPECTED, "counted " + actual + ", more than the " + EXPECTED + " increments performed");
        assertTrue(actual > 0, "expected at least some increments to land");
    }
}
