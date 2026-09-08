package org.alxkm.antipatterns.lockcontention;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.IntSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The three approaches in this package trade contention for throughput in different ways; all
 * three must still count correctly. StampedLock in particular is easy to get wrong, because an
 * optimistic read that fails validation has to be retried under a real read lock.
 */
class LockContentionResolutionTest {
    private static final int THREADS = 8;
    private static final int INCREMENTS = 25_000;
    private static final int EXPECTED = THREADS * INCREMENTS;

    private static Stream<Arguments> implementations() {
        LockContentionExample synchronizedCounter = new LockContentionExample();
        LockContentionResolution atomicCounter = new LockContentionResolution();
        StampedLockExample stampedCounter = new StampedLockExample();
        return Stream.of(
                Arguments.of("synchronized", (Runnable) synchronizedCounter::increment,
                        (IntSupplier) synchronizedCounter::getCounter),
                Arguments.of("AtomicInteger", (Runnable) atomicCounter::increment,
                        (IntSupplier) atomicCounter::getCounter),
                Arguments.of("StampedLock", (Runnable) stampedCounter::increment,
                        (IntSupplier) stampedCounter::getCounter));
    }

    @ParameterizedTest(name = "{0} counts every increment")
    @MethodSource("implementations")
    void everyImplementationCountsCorrectly(String label, Runnable increment, IntSupplier read)
            throws InterruptedException {
        Concurrently.run(THREADS, INCREMENTS, increment);

        assertEquals(EXPECTED, read.getAsInt(), label + " lost increments");
    }
}
