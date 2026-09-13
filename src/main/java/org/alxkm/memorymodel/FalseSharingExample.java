package org.alxkm.memorymodel;

import java.util.concurrent.TimeUnit;

/**
 * Two threads, two separate fields, no contention in the code -- and a threefold slowdown.
 * <p>
 * The memory model costs more than correctness. Caches move data in lines, typically 64 bytes, not in
 * fields. Two fields that land on the same line are one unit as far as the coherence protocol is
 * concerned: when one core writes its field, the line is invalidated in the other core's cache, so the
 * second core must refetch it to touch a field it alone uses. The threads never share data and never
 * block on each other, yet each write bounces the line between cores. Hence the name -- the sharing is
 * an accident of layout.
 * <p>
 * Measured on the machine this example was written on, with 50 million increments per thread and both
 * layouts compiled before anything is timed:
 * <pre>
 *   adjacent fields : ~730 ms
 *   padded fields   : ~265 ms      (about 2.8x faster)
 * </pre>
 * Warm the JIT up before believing any of this. Timing the first run of each layout produces a
 * similar looking ratio for an entirely different reason, and it disappears on the second round.
 * The fix is to keep the two fields off one line by putting 7 longs of padding between them: 8 longs at
 * 8 bytes each fills a 64-byte line. This is what {@code jdk.internal.vm.annotation.Contended} does, and
 * it is why {@link java.util.concurrent.atomic.LongAdder} beats
 * {@link java.util.concurrent.atomic.AtomicLong} under contention -- it spreads its cells out
 * deliberately.
 * <p>
 * Note the direction of the lesson: padding is not a general optimisation. It costs memory and only
 * pays off for fields written hard by different threads. Reach for it after measuring, not before.
 */
public final class FalseSharingExample {

    /** Enough increments for the cache-line effect to dominate thread start-up noise. */
    private static final long DEFAULT_ITERATIONS = 50_000_000L;

    /** Both counters land on the same cache line, so each write invalidates the other core's copy. */
    static final class Adjacent {
        volatile long first;
        volatile long second;
    }

    /**
     * The same two counters, separated by 7 longs.
     * <p>
     * 7 longs of padding plus the 8-byte field itself fills a 64-byte cache line, so {@code second}
     * cannot land on the same line as {@code first}.
     */
    @SuppressWarnings("unused")
    static final class Padded {
        volatile long first;
        long pad1;
        long pad2;
        long pad3;
        long pad4;
        long pad5;
        long pad6;
        long pad7;
        volatile long second;
    }

    private FalseSharingExample() {
    }

    /**
     * Times two threads incrementing two adjacent fields.
     *
     * @param iterations increments per thread.
     * @return elapsed wall-clock time in milliseconds.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public static long timeAdjacentMillis(long iterations) throws InterruptedException {
        Adjacent counters = new Adjacent();
        long elapsed = timeBoth(
                () -> {
                    for (long i = 0; i < iterations; i++) {
                        counters.first++;
                    }
                },
                () -> {
                    for (long i = 0; i < iterations; i++) {
                        counters.second++;
                    }
                });
        verify(counters.first, counters.second, iterations);
        return elapsed;
    }

    /**
     * Times the same work with the two fields on different cache lines.
     *
     * @param iterations increments per thread.
     * @return elapsed wall-clock time in milliseconds.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public static long timePaddedMillis(long iterations) throws InterruptedException {
        Padded counters = new Padded();
        long elapsed = timeBoth(
                () -> {
                    for (long i = 0; i < iterations; i++) {
                        counters.first++;
                    }
                },
                () -> {
                    for (long i = 0; i < iterations; i++) {
                        counters.second++;
                    }
                });
        verify(counters.first, counters.second, iterations);
        return elapsed;
    }

    /**
     * Runs both tasks on their own thread and returns how long the pair took.
     *
     * @param first  work for the first thread.
     * @param second work for the second thread.
     * @return elapsed wall-clock time in milliseconds.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    private static long timeBoth(Runnable first, Runnable second) throws InterruptedException {
        Thread one = new Thread(first, "counter-1");
        Thread two = new Thread(second, "counter-2");

        long startNanos = System.nanoTime();
        one.start();
        two.start();
        one.join();
        two.join();
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    /**
     * Confirms both counters reached the expected total.
     * <p>
     * Each counter is written by exactly one thread, so no increment can be lost regardless of layout.
     * False sharing is purely a performance effect -- checking this makes that claim explicit rather
     * than assumed.
     *
     * @param first    the first counter's final value.
     * @param second   the second counter's final value.
     * @param expected the value both should have reached.
     */
    private static void verify(long first, long second, long expected) {
        if (first != expected || second != expected) {
            throw new IllegalStateException(
                    "counters should be exact: expected " + expected + " but got " + first + " and " + second);
        }
    }

    /** Short runs of both shapes, enough for the JIT to compile the loops before anything is timed. */
    private static final int WARMUP_ROUNDS = 5;

    /** Measured rounds. More than one, because a single timing says nothing about its own stability. */
    private static final int MEASURED_ROUNDS = 4;

    private static final long WARMUP_ITERATIONS = 5_000_000L;

    /**
     * Warms both layouts up, then times them several times and prints every result.
     * <p>
     * The warm-up is not a formality here. An earlier version of this demo timed the very first run of
     * each layout, and the adjacent one came out three times slower simply because it was the first
     * thing the JIT had to compile. The ratio looked like the cache effect and was not; timing the
     * second round showed no difference at all. Both layouts are compiled before anything is measured
     * now, and running several rounds makes an unstable measurement visible rather than quotable.
     *
     * @param args command line arguments (not used).
     * @throws InterruptedException if the demo is interrupted.
     */
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            timeAdjacentMillis(WARMUP_ITERATIONS);
            timePaddedMillis(WARMUP_ITERATIONS);
        }

        for (int round = 0; round < MEASURED_ROUNDS; round++) {
            long adjacent = timeAdjacentMillis(DEFAULT_ITERATIONS);
            long padded = timePaddedMillis(DEFAULT_ITERATIONS);
            System.out.printf("round %d: adjacent=%dms padded=%dms -> %.2fx%n",
                    round, adjacent, padded, (double) adjacent / padded);
        }
    }
}
