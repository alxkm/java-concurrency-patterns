package org.alxkm.patterns.synchronizers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Phaser;

/**
 * Phaser: a reusable barrier whose number of parties can change while it runs.
 * <p>
 * Workers move through phases in step: nobody starts phase n + 1 until everyone still taking part has
 * finished phase n. A CyclicBarrier does the same with a fixed number of parties, and that is where it
 * falls short: a worker with less to do cannot leave, because the barrier would wait for it forever.
 * With a Phaser it calls arriveAndDeregister() and the others carry on without it.
 * <p>
 * Three calls carry the example:
 * <ul>
 *   <li>register() adds a party; it is called for each worker before the worker starts.</li>
 *   <li>arriveAndAwaitAdvance() finishes the phase for this worker and waits for the rest.</li>
 *   <li>arriveAndDeregister() finishes the phase and leaves; it does not wait.</li>
 * </ul>
 * arrive() also exists and does not wait either. The earlier version of this example used it where it
 * meant to wait, so its phaser synchronised nothing.
 */
public final class PhaserExample {

    private PhaserExample() {
    }

    /**
     * One step of one worker, and the phase the phaser was in while the step ran.
     *
     * @param phase  the phaser's phase during the step.
     * @param worker the worker's index.
     */
    public record Step(int phase, int worker) {
    }

    /**
     * Runs one worker per entry in {@code stepsPerWorker}; worker i does that many steps, one per phase,
     * and leaves the phaser after its last one.
     *
     * @param stepsPerWorker how many steps each worker does; each at least 1.
     * @return every step, in the order they were done.
     * @throws InterruptedException if interrupted while waiting for the workers.
     */
    public static List<Step> run(int... stepsPerWorker) throws InterruptedException {
        Phaser phaser = new Phaser();
        List<Step> log = Collections.synchronizedList(new ArrayList<>());
        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < stepsPerWorker.length; i++) {
            int worker = i;
            int steps = stepsPerWorker[i];
            if (steps < 1) {
                throw new IllegalArgumentException("worker " + i + " needs at least one step: " + steps);
            }
            // Every worker is registered before any starts. Registering and starting in the same iteration
            // would let the first worker arrive while it was still the only party, and advance phase 0 alone.
            phaser.register();
            workers.add(new Thread(() -> {
                for (int step = 0; step < steps; step++) {
                    log.add(new Step(phaser.getPhase(), worker));
                    if (step < steps - 1) {
                        phaser.arriveAndAwaitAdvance();
                    } else {
                        phaser.arriveAndDeregister();
                    }
                }
            }, "phaser-worker-" + i));
        }

        workers.forEach(Thread::start);
        for (Thread worker : workers) {
            worker.join();
        }
        return List.copyOf(log);
    }

    public static void main(String[] args) throws InterruptedException {
        // Worker 0 has one step, worker 1 two, worker 2 four. Each leaves after its last step.
        for (Step step : run(1, 2, 4)) {
            System.out.println("phase " + step.phase() + ": worker " + step.worker());
        }
    }
}
