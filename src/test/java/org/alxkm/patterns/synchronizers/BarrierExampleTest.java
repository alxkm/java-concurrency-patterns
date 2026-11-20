package org.alxkm.patterns.synchronizers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for the round-by-round sum. Calls that might hang if the barrier is mishandled run on a daemon
 * thread with a timeout, so a bug fails the test instead of hanging the build.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class BarrierExampleTest {

    @Test
    void eachRoundIsTotalledFromEveryWorkersShare() throws Exception {
        long[][] rounds = new long[100][];
        for (int r = 0; r < rounds.length; r++) {
            rounds[r] = LongStream.rangeClosed(1, 50).map(v -> v * 7 + 3L).toArray();
        }
        long[] expected = Arrays.stream(rounds).mapToLong(values -> Arrays.stream(values).sum()).toArray();

        assertArrayEquals(expected, withTimeout(() -> BarrierExample.totalsByRound(rounds, 4)));
    }

    /**
     * Each round has different numbers, so a total written to the wrong round, or a round merged before
     * every worker had written its part, would show up as a wrong value.
     */
    @Test
    void roundsDoNotBleedIntoEachOther() throws Exception {
        long[][] rounds = new long[200][];
        for (int r = 0; r < rounds.length; r++) {
            long base = r * 1_000L;
            rounds[r] = LongStream.range(0, 8).map(v -> base + v).toArray();
        }
        long[] expected = Arrays.stream(rounds).mapToLong(values -> Arrays.stream(values).sum()).toArray();

        assertArrayEquals(expected, withTimeout(() -> BarrierExample.totalsByRound(rounds, 8)));
    }

    /**
     * One worker overflows; the call must report it, not hang. Which worker fails and whether the others
     * are already waiting varies, so this runs many times: the others may be at the barrier when the
     * failure happens, or still on their way to it.
     */
    @Test
    void aFailingWorkerStopsTheOthersInsteadOfLeavingThemWaiting() throws Exception {
        for (int run = 0; run < 300; run++) {
            int failing = run % 3;
            // Three workers: worker w adds the numbers at w and w + 3, so only the failing one overflows.
            long[] second = new long[6];
            second[failing] = Long.MAX_VALUE;
            second[failing + 3] = 1;
            long[][] rounds = {{1, 2, 3}, second};

            ExecutionException thrown = assertThrows(ExecutionException.class,
                    () -> withTimeout(() -> BarrierExample.totalsByRound(rounds, 3)), "run " + run);
            assertInstanceOf(IllegalStateException.class, thrown.getCause());
            assertInstanceOf(ArithmeticException.class, thrown.getCause().getCause());
        }
    }

    /**
     * Why breakForEveryone does not use reset(): a party that arrives after a reset waits on a fresh
     * barrier. After breakForEveryone it gets BrokenBarrierException at once.
     */
    @Test
    void aBrokenBarrierStaysBrokenForLaterArrivals() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        BarrierExample.breakForEveryone(barrier);

        assertTrue(barrier.isBroken());
        assertThrows(BrokenBarrierException.class, barrier::await);
        assertFalse(Thread.currentThread().isInterrupted(), "breakForEveryone should clear the flag it set");
    }

    @Test
    void afterAResetALaterArrivalWaitsInstead() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        barrier.reset();

        assertThrows(TimeoutException.class, () -> barrier.await(50, TimeUnit.MILLISECONDS));
    }

    @Test
    void mainRuns() throws Exception {
        withTimeout(() -> {
            BarrierExample.main(new String[0]);
            return null;
        });
    }

    private static <T> T withTimeout(Callable<T> call) throws Exception {
        ExecutorService runner = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "barrier-test");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<T> result = runner.submit(call);
            try {
                return result.get(20, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("did not finish: the barrier is probably still waiting for a worker that failed");
                return null;
            }
        } finally {
            runner.shutdownNow();
        }
    }
}
