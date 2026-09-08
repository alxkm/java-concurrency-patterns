package org.alxkm.antipatterns.excessivesynchronization;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.IntSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * All three counters here are correct; the package is about the cost of the synchronization, not
 * its absence. These tests pin that correctness down so a later optimisation cannot quietly trade
 * it away -- which is the usual way "reduce the locking" goes wrong.
 */
class CounterVariantsTest {
    private static final int THREADS = 4;
    private static final int INCREMENTS = 50_000;
    private static final int EXPECTED = THREADS * INCREMENTS;

    private static Stream<Arguments> counters() {
        ExcessiveSyncCounter excessive = new ExcessiveSyncCounter();
        OptimizedCounter optimized = new OptimizedCounter();
        AtomicCounter atomic = new AtomicCounter();
        return Stream.of(
                Arguments.of("synchronized method", (Runnable) excessive::increment,
                        (IntSupplier) excessive::getCount),
                Arguments.of("synchronized block", (Runnable) optimized::increment,
                        (IntSupplier) optimized::getCount),
                Arguments.of("AtomicInteger", (Runnable) atomic::increment,
                        (IntSupplier) atomic::getCount));
    }

    @ParameterizedTest(name = "{0} counts every increment")
    @MethodSource("counters")
    void everyVariantCountsEveryIncrement(String label, Runnable increment, IntSupplier read)
            throws InterruptedException {
        Concurrently.run(THREADS, INCREMENTS, increment);

        assertEquals(EXPECTED, read.getAsInt(), label + " lost increments");
    }
}
