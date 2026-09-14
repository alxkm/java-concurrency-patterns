package org.alxkm.patterns.executors;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the usage of ScheduledThreadPoolExecutor to schedule tasks.
 */
public class ScheduledThreadPoolExecutorExample {

    /**
     *
     * We create a ScheduledThreadPoolExecutor with 3 threads.
     * We schedule three different tasks:
     * Task 1: Executes after a delay of 1 second using schedule().
     * Task 2: Executes repeatedly with a fixed rate, starting after an initial delay of 2 seconds, with a period of 3 seconds using scheduleAtFixedRate().
     * Task 3: Executes repeatedly with a fixed delay between termination of one execution and commencement of the next, starting after an initial delay of 2 seconds, with a delay of 3 seconds using scheduleWithFixedDelay().
     * Finally, we schedule a task to shut down the executor after 10 seconds using schedule().
     *
     */
    /** Timings used by the demo in main, in milliseconds. */
    private static final long DEMO_UNIT_MILLIS = 1000;

    /**
     * How many times each scheduled task ran before the executor was stopped.
     *
     * @param oneShot        runs of the one-off task; exactly 1 once its delay has passed.
     * @param atFixedRate    runs of the scheduleAtFixedRate task.
     * @param withFixedDelay runs of the scheduleWithFixedDelay task.
     */
    public record Runs(int oneShot, int atFixedRate, int withFixedDelay) {
    }

    public static void main(String[] args) throws InterruptedException {
        Runs runs = run(DEMO_UNIT_MILLIS, 10 * DEMO_UNIT_MILLIS);
        System.out.printf("one-shot: %d, fixed rate: %d, fixed delay: %d%n",
                runs.oneShot(), runs.atFixedRate(), runs.withFixedDelay());
    }

    /**
     * Schedules the three kinds of task, lets them run, then shuts the executor down.
     *
     * The difference between the two repeating forms is what the period is measured from.
     * scheduleAtFixedRate starts each run one period after the previous run STARTED, so a task that
     * overruns its period has no gap at all and the runs bunch up. scheduleWithFixedDelay waits one
     * period after the previous run FINISHED, so the gap is always there and the rate drifts instead.
     * Pick the first when you need a rate, the second when you need breathing room.
     *
     * @param unitMillis  the base interval: the one-shot task fires after this, the repeating ones
     *                    start after twice this and then repeat every three times this.
     * @param runForMillis how long to let the executor run before shutting it down.
     * @return how many times each task ran.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static Runs run(long unitMillis, long runForMillis) throws InterruptedException {
        AtomicInteger oneShot = new AtomicInteger();
        AtomicInteger atFixedRate = new AtomicInteger();
        AtomicInteger withFixedDelay = new AtomicInteger();

        // Create a ScheduledThreadPoolExecutor with 3 threads
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);
        try {
            // A single run, once the delay has passed
            executor.schedule(() -> {
                oneShot.incrementAndGet();
                System.out.println("Task 1 executed after the initial delay.");
            }, unitMillis, TimeUnit.MILLISECONDS);

            // Repeats measured from the start of the previous run
            executor.scheduleAtFixedRate(() -> {
                atFixedRate.incrementAndGet();
                System.out.println("Task 2 executed repeatedly with a fixed rate.");
            }, 2 * unitMillis, 3 * unitMillis, TimeUnit.MILLISECONDS);

            // Repeats measured from the end of the previous run
            executor.scheduleWithFixedDelay(() -> {
                withFixedDelay.incrementAndGet();
                System.out.println("Task 3 executed repeatedly with a fixed delay.");
            }, 2 * unitMillis, 3 * unitMillis, TimeUnit.MILLISECONDS);

            Thread.sleep(runForMillis);
        } finally {
            // shutdown() alone would let the repeating tasks keep going until their next fire;
            // shutdownNow() cancels them.
            executor.shutdownNow();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("scheduler did not terminate");
            }
        }
        return new Runs(oneShot.get(), atFixedRate.get(), withFixedDelay.get());
    }
}

