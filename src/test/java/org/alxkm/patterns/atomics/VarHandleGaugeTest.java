package org.alxkm.patterns.atomics;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link VarHandleGauge}.
 * <p>
 * The two updates fail in different ways when they are not atomic, so each gets its own check. A lost
 * {@code getAndAdd} leaves in-flight away from zero after balanced enters and exits. A lost peak update
 * leaves the peak below a number every task was held long enough to reach.
 */
class VarHandleGaugeTest {

    @Test
    void peakIsExactlyTheNumberOfTasksHeldInsideAtOnce() {
        VarHandleGauge gauge = VarHandleGauge.measure(200);

        assertEquals(200, gauge.peak(), "every task was inside when the last one arrived");
        assertEquals(0, gauge.inFlight(), "every task that entered also left");
    }

    @Test
    void balancedEntersAndExitsReturnToZeroUnderContention() throws InterruptedException {
        VarHandleGauge gauge = new VarHandleGauge();
        int threads = 8;

        Concurrently.run(threads, 20_000, () -> {
            gauge.enter();
            gauge.exit();
        });

        assertEquals(0, gauge.inFlight(), "getAndAdd is atomic, so no enter or exit may be lost");
        assertTrue(gauge.peak() >= 1 && gauge.peak() <= threads,
                () -> "peak must lie between one and the thread count, was " + gauge.peak());
    }

    @Test
    void peakNeverGoesDown() {
        VarHandleGauge gauge = new VarHandleGauge();
        gauge.enter();
        gauge.enter();
        gauge.exit();
        gauge.exit();
        gauge.enter();

        assertEquals(2, gauge.peak(), "a later, lower level must not overwrite the high-water mark");
        assertEquals(1, gauge.inFlight(), "two in, two out, one in");
    }
}
