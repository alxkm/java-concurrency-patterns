package org.alxkm.patterns.atomics;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AtomicExample}.
 * <p>
 * Each example runs a fixed sequence of operations, so its end state is fully determined. Asserting
 * that end state pins down every step: if the compare-and-set in the integer example stopped applying,
 * the value would stay at 16 instead of reaching 42.
 */
class AtomicExampleTest {

    @Test
    void atomicBooleanEndsTrueAfterGetAndSetThenCompareAndSet() {
        // true -> getAndSet(false) -> false -> compareAndSet(false, true) -> true
        assertTrue(AtomicExample.atomicBooleanExample(),
                "the CAS from false to true should have applied");
    }

    @Test
    void atomicIntegerAppliesIncrementAddAndCompareAndSet() {
        // 10 -> incrementAndGet -> 11 -> addAndGet(5) -> 16 -> compareAndSet(16, 42) -> 42
        assertEquals(42, AtomicExample.atomicIntegerExample());
    }

    @Test
    void atomicLongAppliesDecrementAndUpdate() {
        // 100 -> decrementAndGet -> 99 -> updateAndGet(v -> v * 2) -> 198
        assertEquals(198L, AtomicExample.atomicLongExample());
    }

    @Test
    void atomicIntegerArrayAppliesGetAndAddThenCompareAndSet() {
        // {1, 2, 3} -> getAndAdd(1, 5) -> {1, 7, 3} -> compareAndSet(0, 1, 10) -> {10, 7, 3}
        assertArrayEquals(new int[] {10, 7, 3}, AtomicExample.atomicIntegerArrayExample());
    }

    @Test
    void atomicLongArrayAppliesCompareAndSetThenIncrementsAll() {
        // {100, 200, 300} -> compareAndSet(1, 200, 250) -> {100, 250, 300} -> increment all
        assertArrayEquals(new long[] {101, 251, 301}, AtomicExample.atomicLongArrayExample());
    }

    /**
     * The point of an atomic: a read-modify-write from many threads loses nothing.
     * <p>
     * This is the property the examples above exist to demonstrate, so it is worth checking directly.
     * A plain {@code int} in the same loop would come out short; see
     * {@code org.alxkm.antipatterns.nonatomiccompoundactions} for that side of it.
     */
    @Test
    void incrementAndGetLosesNoUpdatesUnderContention() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        int threads = 8;
        int iterations = 10_000;

        Concurrently.run(threads, iterations, counter::incrementAndGet);

        assertEquals(threads * iterations, counter.get(),
                "incrementAndGet is atomic, so no increment may be lost");
    }

    /**
     * compareAndSet must succeed for exactly one of the threads racing to claim the same value.
     */
    @Test
    void compareAndSetSucceedsForExactlyOneThread() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        int threads = 8;

        List<Boolean> results = Concurrently.collect(threads, () -> counter.compareAndSet(0, 1));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count(),
                "only one thread can win a CAS from the same expected value");
        assertEquals(1, counter.get());
    }
}
