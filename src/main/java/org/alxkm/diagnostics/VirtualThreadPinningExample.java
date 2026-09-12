package org.alxkm.diagnostics;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Virtual thread pinning: the one place where {@code synchronized} and {@code ReentrantLock} stop being
 * interchangeable.
 * <p>
 * A virtual thread that blocks normally unmounts from its carrier, which is what lets a handful of
 * carriers serve thousands of virtual threads. Blocking inside a {@code synchronized} block is the
 * exception: on Java 21 the monitor is tied to the carrier, so the virtual thread cannot unmount and
 * holds the carrier for the whole time it is blocked. That is pinning.
 * <p>
 * The cost is not lock contention. In the measurement below every task locks a monitor of its own, so
 * no task ever waits for another task's lock. What runs out is carriers: the scheduler defaults to one
 * per core, so only that many tasks can be in flight at once.
 * <pre>
 *   64 tasks, each blocking 500ms, on a 12 core machine:
 *     synchronized  : ~3000 ms    12 at a time, so 6 rounds
 *     ReentrantLock : ~500 ms     all 64 at once
 * </pre>
 *
 * <h2>How to find it in your own code</h2>
 * Pinning is invisible in a thread dump; the symptom is throughput that refuses to scale. Two ways to
 * see it directly:
 * <ul>
 *   <li>{@code -Djdk.tracePinnedThreads=short} prints the stack frame that holds the monitor whenever a
 *       pinned thread blocks. Running this class with it produces a line ending in
 *       {@code <== monitors:1} pointing straight at the synchronized block.</li>
 *   <li>The {@code jdk.VirtualThreadPinned} JFR event records the same thing with less overhead, which
 *       makes it the option for a production process.</li>
 * </ul>
 *
 * <h2>What to do about it</h2>
 * Replace the monitor with a {@link ReentrantLock}, which a virtual thread can hold across an unmount.
 * The rest of the time {@code synchronized} is fine, including when nothing blocks inside it.
 * <p>
 * Note the expiry date on this advice. JEP 491 removed monitor pinning in Java 24, so on a recent JDK
 * both versions below run in the same time. It still matters for anything on 21, which is the current
 * LTS and what this repository builds against.
 */
public final class VirtualThreadPinningExample {

    /** Comfortably more tasks than any machine has cores, so the carrier limit shows up. */
    private static final int DEFAULT_TASKS = 64;

    /** Long enough to dominate scheduling noise, short enough to keep the demo quick. */
    private static final long DEFAULT_BLOCK_MILLIS = 500;

    private VirtualThreadPinningExample() {
    }

    /**
     * How long the same blocking workload took under each kind of lock.
     *
     * @param synchronizedMillis elapsed time when the work blocks inside a synchronized block.
     * @param reentrantLockMillis elapsed time when it blocks while holding a ReentrantLock.
     */
    public record Timings(long synchronizedMillis, long reentrantLockMillis) {
    }

    /**
     * Runs the workload both ways at the default size.
     *
     * @return the two timings.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static Timings compare() throws InterruptedException {
        return compare(DEFAULT_TASKS, DEFAULT_BLOCK_MILLIS);
    }

    /**
     * Runs the workload both ways.
     *
     * @param tasks       how many virtual threads to run.
     * @param blockMillis how long each one blocks while holding its lock.
     * @return the two timings.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static Timings compare(int tasks, long blockMillis) throws InterruptedException {
        return new Timings(
                runPinned(tasks, blockMillis),
                runUnpinned(tasks, blockMillis));
    }

    /**
     * Blocks inside a synchronized block, which pins the carrier on Java 21.
     *
     * @param tasks       how many virtual threads to run.
     * @param blockMillis how long each one blocks.
     * @return elapsed wall clock time in milliseconds.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static long runPinned(int tasks, long blockMillis) throws InterruptedException {
        return timeAll(tasks, blockMillis, (monitor, lock, millis) -> {
            synchronized (monitor) {
                sleep(millis);
            }
        });
    }

    /**
     * Blocks while holding a ReentrantLock, which a virtual thread can carry across an unmount.
     *
     * @param tasks       how many virtual threads to run.
     * @param blockMillis how long each one blocks.
     * @return elapsed wall clock time in milliseconds.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public static long runUnpinned(int tasks, long blockMillis) throws InterruptedException {
        return timeAll(tasks, blockMillis, (monitor, lock, millis) -> {
            lock.lock();
            try {
                sleep(millis);
            } finally {
                lock.unlock();
            }
        });
    }

    /** The body each task runs, holding a lock of its own while it blocks. */
    @FunctionalInterface
    private interface Body {
        void run(Object monitor, ReentrantLock lock, long blockMillis);
    }

    /**
     * Runs the body on one virtual thread per task and times the lot.
     * <p>
     * Each task gets its own monitor and its own lock, so nothing here contends. Any difference between
     * the two variants comes from carrier availability alone.
     *
     * @param tasks       how many virtual threads to run.
     * @param blockMillis how long each one blocks.
     * @param body        the work to run.
     * @return elapsed wall clock time in milliseconds.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    private static long timeAll(int tasks, long blockMillis, Body body) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(tasks);
        long startNanos = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> {
                    body.run(new Object(), new ReentrantLock(), blockMillis);
                    done.countDown();
                });
            }
            if (!done.await(2, TimeUnit.MINUTES)) {
                throw new IllegalStateException("tasks did not finish: " + done.getCount() + " left");
            }
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    /**
     * Sleeps, restoring the interrupt flag rather than propagating a checked exception.
     *
     * @param millis how long to sleep.
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Runs both variants and prints the comparison.
     * <p>
     * Add {@code -Djdk.tracePinnedThreads=short} to see the pinned frames as they happen.
     *
     * @param args command line arguments (not used).
     * @throws InterruptedException if the demo is interrupted.
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("cores: " + Runtime.getRuntime().availableProcessors()
                + ", tasks: " + DEFAULT_TASKS + ", each blocking " + DEFAULT_BLOCK_MILLIS + "ms");

        Timings timings = compare();
        System.out.printf("synchronized  : %d ms%n", timings.synchronizedMillis());
        System.out.printf("ReentrantLock : %d ms%n", timings.reentrantLockMillis());
        System.out.printf("ratio         : %.1fx%n",
                (double) timings.synchronizedMillis() / Math.max(1, timings.reentrantLockMillis()));
    }
}
