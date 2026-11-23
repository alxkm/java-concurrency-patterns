package org.alxkm.patterns.forkjoinpool;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForkJoinPoolExampleTest {

    private record Range(int from, int to) {
    }

    /**
     * Sizes around the threshold and odd sizes at every level, where splitting by size instead of by
     * index used to drop elements.
     */
    @Test
    void theSumMatchesASequentialSum() {
        int[] sizes = {0, 1, 2, 3, 7, 999, 1_000, 1_001, 65_537, 123_457};
        int[] thresholds = {1, 3, 1_000};
        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            for (int size : sizes) {
                long[] values = LongStream.range(0, size).map(v -> v * 31 - 7).toArray();
                long expected = LongStream.of(values).sum();
                for (int threshold : thresholds) {
                    assertEquals(expected, ForkJoinPoolExample.sum(values, threshold, pool, (from, to) -> { }),
                            "size " + size + ", threshold " + threshold);
                }
            }
        }
    }

    /**
     * The leaves must cover the array exactly: no gap, no overlap, none larger than the threshold.
     */
    @Test
    void everyElementIsInExactlyOneLeaf() {
        int size = 100_003;
        int threshold = 777;
        List<Range> leaves = Collections.synchronizedList(new ArrayList<>());

        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            ForkJoinPoolExample.sum(new long[size], threshold, pool, (from, to) -> leaves.add(new Range(from, to)));
        }

        List<Range> sorted = new ArrayList<>(leaves);
        sorted.sort(Comparator.comparingInt(Range::from));
        int next = 0;
        for (Range leaf : sorted) {
            assertEquals(next, leaf.from(), "the leaf after " + next + " starts elsewhere");
            assertTrue(leaf.to() > leaf.from(), "empty leaf " + leaf);
            assertTrue(leaf.to() - leaf.from() <= threshold, "leaf larger than the threshold: " + leaf);
            next = leaf.to();
        }
        assertEquals(size, next, "the leaves stop before the end");
        assertTrue(sorted.size() >= size / threshold, "the work was split: " + sorted.size() + " leaves");
    }

    /**
     * The overflow happens in one leaf deep in the tree, on some worker thread; it still reaches the
     * caller of sum(). The JDK may hand over a copy with the original as its cause, so the original's
     * message is looked for on either.
     */
    @Test
    void anExceptionInASubtaskReachesTheCaller() {
        long[] values = new long[10_000];
        values[6_000] = Long.MAX_VALUE;
        values[6_001] = 1;

        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            ArithmeticException thrown = assertThrows(ArithmeticException.class,
                    () -> ForkJoinPoolExample.sum(values, 100, pool, (from, to) -> { }));
            Throwable original = thrown.getCause() instanceof ArithmeticException ? thrown.getCause() : thrown;
            assertEquals("long overflow", original.getMessage());
        }
    }

    /**
     * Each half fits in a long, the total does not: the overflow is caught where the halves are joined.
     */
    @Test
    void anOverflowWhenJoiningTheHalvesIsCaughtToo() {
        long[] values = {Long.MAX_VALUE / 2 + 1, Long.MAX_VALUE / 2 + 1};

        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            assertThrows(ArithmeticException.class,
                    () -> ForkJoinPoolExample.sum(values, 1, pool, (from, to) -> { }));
        }
    }

    @Test
    void thePublicSumUsesAPoolOfItsOwn() {
        long[] values = LongStream.rangeClosed(1, 200_000).toArray();

        assertEquals(200_000L * 200_001L / 2, ForkJoinPoolExample.sum(values, 3));
    }

    @Test
    void aThresholdBelowOneIsRejected() {
        try (ForkJoinPool pool = new ForkJoinPool(1)) {
            assertThrows(IllegalArgumentException.class,
                    () -> ForkJoinPoolExample.sum(new long[1], 0, pool, (from, to) -> { }));
        }
    }

    @Test
    void mainRuns() {
        ForkJoinPoolExample.main(new String[0]);
    }
}
