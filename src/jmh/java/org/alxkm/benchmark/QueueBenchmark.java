package org.alxkm.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Offer and poll across the queue implementations this repository documents.
 * <p>
 * Two claims in the README come from an article written in 2012 and have never been checked here:
 * that {@code ConcurrentLinkedDeque} costs "a 40% performance loss" against
 * {@code ConcurrentLinkedQueue}, and that {@code LinkedBlockingQueue} "offers higher performance" than
 * {@code ArrayBlockingQueue} because it uses two locks instead of one. Both are plausible and neither
 * is worth repeating without a measurement on the hardware in front of you.
 * <p>
 * Each benchmark offers an element and immediately polls it back, so the queue stays near empty and the
 * measurement is of the enqueue and dequeue path rather than of memory growth. The bounded queues are
 * given ample capacity for the same reason: a full queue measures blocking, which is a different
 * question.
 * <p>
 * Run contended, since an uncontended queue is not what any of these classes exist for:
 * <pre>
 *   ./gradlew jmh -PjmhArgs="QueueBenchmark -t 4"
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class QueueBenchmark {

    /** Large enough that the bounded queues never block during the run. */
    private static final int CAPACITY = 100_000;

    private Queue<Integer> concurrentLinkedQueue;
    private Queue<Integer> concurrentLinkedDeque;
    private BlockingQueue<Integer> linkedBlockingQueue;
    private BlockingQueue<Integer> arrayBlockingQueue;

    @Setup
    public void setUp() {
        concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
        concurrentLinkedDeque = new ConcurrentLinkedDeque<>();
        linkedBlockingQueue = new LinkedBlockingQueue<>(CAPACITY);
        arrayBlockingQueue = new ArrayBlockingQueue<>(CAPACITY);
    }

    @Benchmark
    public Integer concurrentLinkedQueueOfferPoll() {
        concurrentLinkedQueue.offer(1);
        return concurrentLinkedQueue.poll();
    }

    @Benchmark
    public Integer concurrentLinkedDequeOfferPoll() {
        concurrentLinkedDeque.offer(1);
        return concurrentLinkedDeque.poll();
    }

    @Benchmark
    public Integer linkedBlockingQueueOfferPoll() {
        linkedBlockingQueue.offer(1);
        return linkedBlockingQueue.poll();
    }

    @Benchmark
    public Integer arrayBlockingQueueOfferPoll() {
        arrayBlockingQueue.offer(1);
        return arrayBlockingQueue.poll();
    }
}
