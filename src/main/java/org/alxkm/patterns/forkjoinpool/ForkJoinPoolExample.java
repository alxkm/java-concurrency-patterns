package org.alxkm.patterns.forkjoinpool;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Fork/Join with a result: a RecursiveTask that sums an array.
 * <p>
 * A task splits its range in two until the range is small enough to add up directly. Three details
 * decide whether that is correct and whether it is any faster:
 * <ul>
 *   <li><b>Split by index, not by size.</b> The halves are [from, mid) and [mid, to), so every element
 *   lands in exactly one of them. The earlier version of this example made two subtasks of size
 *   workload / 2; for an odd workload one element simply disappeared, and its result meant nothing.</li>
 *   <li><b>Fork one half, compute the other.</b> fork() puts the left half on this worker's queue,
 *   where an idle worker can steal it. This thread then works on the right half itself instead of
 *   forking it too and waiting, and only joins the left half at the end. If nobody stole it, join() runs
 *   it here.</li>
 *   <li><b>Stop splitting early.</b> Below the threshold a plain loop is far cheaper than more tasks.
 *   {@link ForkJoinMergeSort} explains the threshold in more detail.</li>
 * </ul>
 * An exception in any subtask reaches the caller: join() and invoke() rethrow it. Here the sum uses
 * Math.addExact, so an overflow anywhere in the tree comes out of {@link #sum} as ArithmeticException
 * instead of a wrong total. When it was thrown on another thread, what the caller gets is usually not
 * the original but a new exception of the same type, with the original as its cause. That way the stack
 * trace shows where the caller was waiting as well as where the worker failed; the message is on the
 * cause.
 */
public final class ForkJoinPoolExample {

    /**
     * Ranges at or below this many elements are added up with a loop.
     */
    public static final int DEFAULT_THRESHOLD = 10_000;

    private ForkJoinPoolExample() {
    }

    /**
     * Called with each range a leaf task adds up. Lets a test check how the work was split.
     */
    @FunctionalInterface
    interface LeafObserver {
        void leaf(int from, int to);
    }

    /**
     * Sums the values on a pool of its own.
     *
     * @param values      the values to add up.
     * @param parallelism how many worker threads the pool has.
     * @return the sum.
     * @throws ArithmeticException if the sum overflows a long.
     */
    public static long sum(long[] values, int parallelism) {
        try (ForkJoinPool pool = new ForkJoinPool(parallelism)) {
            return sum(values, DEFAULT_THRESHOLD, pool, (from, to) -> { });
        }
    }

    static long sum(long[] values, int threshold, ForkJoinPool pool, LeafObserver observer) {
        if (threshold < 1) {
            throw new IllegalArgumentException("threshold must be at least 1: " + threshold);
        }
        return pool.invoke(new SumTask(values, 0, values.length, threshold, observer));
    }

    /**
     * Adds up values[from] to values[to - 1].
     */
    static final class SumTask extends RecursiveTask<Long> {
        private static final long serialVersionUID = 1L;

        private final long[] values;
        private final int from;
        private final int to;
        private final int threshold;
        private final transient LeafObserver observer;

        SumTask(long[] values, int from, int to, int threshold, LeafObserver observer) {
            this.values = values;
            this.from = from;
            this.to = to;
            this.threshold = threshold;
            this.observer = observer;
        }

        @Override
        protected Long compute() {
            if (to - from <= threshold) {
                observer.leaf(from, to);
                long sum = 0;
                for (int i = from; i < to; i++) {
                    sum = Math.addExact(sum, values[i]);
                }
                return sum;
            }
            int mid = (from + to) >>> 1;
            SumTask left = new SumTask(values, from, mid, threshold, observer);
            SumTask right = new SumTask(values, mid, to, threshold, observer);
            left.fork();                        // may be stolen by an idle worker
            long rightSum = right.compute();    // this thread keeps working
            return Math.addExact(left.join(), rightSum); // runs left here if nobody stole it
        }
    }

    public static void main(String[] args) {
        long[] values = new long[1_000_001];
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
        System.out.println("0 + 1 + ... + 1,000,000 on 4 workers: " + sum(values, 4)
                + " (expected " + 1_000_000L * 1_000_001L / 2 + ")");

        try {
            sum(new long[]{Long.MAX_VALUE, 1}, 4);
        } catch (ArithmeticException e) {
            System.out.println("Long.MAX_VALUE + 1 threw in a worker; invoke() rethrew " + e
                    + ", caused by " + e.getCause());
        }
    }
}
