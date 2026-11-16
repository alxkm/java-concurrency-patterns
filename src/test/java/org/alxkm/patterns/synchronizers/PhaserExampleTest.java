package org.alxkm.patterns.synchronizers;

import org.alxkm.patterns.synchronizers.PhaserExample.Step;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class PhaserExampleTest {

    /**
     * Each step lands in the phase of its own number: a worker's step n runs in phase n. If a worker ran
     * ahead without waiting, it would do step 1 while the phaser was still in phase 0.
     */
    @Test
    void everyStepRunsInItsOwnPhase() throws InterruptedException {
        int workers = 4;
        int steps = 50;
        int[] stepsPerWorker = new int[workers];
        Arrays.fill(stepsPerWorker, steps);

        List<Step> log = PhaserExample.run(stepsPerWorker);

        assertEquals(workers * steps, log.size());
        for (int worker = 0; worker < workers; worker++) {
            int w = worker;
            List<Integer> phases = log.stream().filter(s -> s.worker() == w).map(Step::phase).toList();
            for (int step = 0; step < steps; step++) {
                assertEquals(step, phases.get(step), "worker " + worker + " step " + step);
            }
        }
    }

    /**
     * The barrier itself: in the order the steps happened, phases never go back. Every step of phase n
     * comes before any step of phase n + 1.
     */
    @Test
    void noWorkerStartsTheNextPhaseBeforeTheOthersFinish() throws InterruptedException {
        List<Step> log = PhaserExample.run(5, 5, 5, 5);

        for (int i = 1; i < log.size(); i++) {
            assertTrue(log.get(i - 1).phase() <= log.get(i).phase(), "out of order at " + i + ": " + log);
        }
    }

    /**
     * What a CyclicBarrier cannot do: workers with less to do leave, and the rest carry on without them.
     * With a fixed party count this would hang in phase 1, waiting for worker 0 forever.
     */
    @Test
    void workersThatLeaveEarlyDoNotHoldUpTheRest() throws InterruptedException {
        List<Step> log = PhaserExample.run(1, 2, 4);

        assertEquals(List.of(
                        List.of(0),
                        List.of(0, 1),
                        List.of(0, 1, 2, 3)),
                List.of(phasesOf(log, 0), phasesOf(log, 1), phasesOf(log, 2)));
    }

    @Test
    void aWorkerWithNoStepsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PhaserExample.run(1, 0));
    }

    @Test
    void mainRuns() throws InterruptedException {
        PhaserExample.main(new String[0]);
    }

    private static List<Integer> phasesOf(List<Step> log, int worker) {
        return log.stream().filter(s -> s.worker() == worker).map(Step::phase).toList();
    }
}
