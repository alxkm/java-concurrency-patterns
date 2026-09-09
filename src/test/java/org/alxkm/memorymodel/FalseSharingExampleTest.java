package org.alxkm.memorymodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * False sharing is a performance effect, not a correctness one, which shapes what can be asserted here.
 * <p>
 * The counters are exact either way -- each is written by exactly one thread -- and
 * {@link FalseSharingExample} verifies that internally, so a wrong answer fails the run. What cannot be
 * asserted is the speed-up: it depends on cache line size, core topology and what else the machine is
 * doing, and a CI runner sharing a host can easily produce noise larger than the effect. Turning a
 * measurement into a hard threshold is how a suite becomes flaky.
 * <p>
 * So the test pins down the part that is guaranteed and reports the part that is measured.
 */
class FalseSharingExampleTest {

    /**
     * Small enough to keep the suite fast; the demo in {@code main} uses 50 million for a clean signal.
     */
    private static final long ITERATIONS = 2_000_000L;

    @Test
    void bothLayoutsCountExactlyAndTheRatioIsReported() throws InterruptedException {
        // Warm up so the timings below are not dominated by interpretation and JIT compilation.
        FalseSharingExample.timeAdjacentMillis(ITERATIONS);
        FalseSharingExample.timePaddedMillis(ITERATIONS);

        long adjacent = FalseSharingExample.timeAdjacentMillis(ITERATIONS);
        long padded = FalseSharingExample.timePaddedMillis(ITERATIONS);

        // timeXxxMillis throws if a counter came out wrong, so reaching here proves both were exact.
        assertTrue(adjacent >= 0 && padded >= 0, "both layouts should complete and count exactly");

        System.out.printf("false sharing: adjacent=%dms padded=%dms -> %.2fx%n",
                adjacent, padded, padded == 0 ? Double.NaN : (double) adjacent / padded);
    }
}
