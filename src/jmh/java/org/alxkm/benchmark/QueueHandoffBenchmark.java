package org.alxkm.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * ArrayBlockingQueue against LinkedBlockingQueue with producers and consumers on separate threads.
 * <p>
 * This exists because {@link QueueBenchmark} does not settle the claim it looks like it settles. There,
 * one thread offers and immediately polls, so the queue never has a producer on one end and a consumer
 * on the other. The argument for LinkedBlockingQueue is specifically that its two locks let a put and a
 * take proceed at the same time, and a single-threaded offer-then-poll loop cannot show that either way.
 * <p>
 * Here four threads offer and four take, which is the shape the claim is about. Both queues get the
 * same generous capacity so the run measures the handoff rather than blocking on a full queue.
 * <pre>
 *   ./gradlew jmh -PjmhArgs="QueueHandoffBenchmark"
 * </pre>
 * The trade the numbers show up is allocation against lock granularity. LinkedBlockingQueue allocates a
 * node per element; ArrayBlockingQueue writes into a ring buffer it allocated once, and pays for it with
 * a single lock shared by both ends.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Group)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class QueueHandoffBenchmark {

    /** Deep enough that producers do not block on a full queue during the run. */
    private static final int CAPACITY = 100_000;

    private BlockingQueue<Integer> arrayQueue;
    private BlockingQueue<Integer> linkedQueue;

    @Setup
    public void setUp() {
        arrayQueue = new ArrayBlockingQueue<>(CAPACITY);
        linkedQueue = new LinkedBlockingQueue<>(CAPACITY);
    }

    @Benchmark
    @Group("arrayBlockingQueue")
    @GroupThreads(4)
    public boolean arrayProduce() {
        return arrayQueue.offer(1);
    }

    @Benchmark
    @Group("arrayBlockingQueue")
    @GroupThreads(4)
    public Integer arrayConsume() {
        return arrayQueue.poll();
    }

    @Benchmark
    @Group("linkedBlockingQueue")
    @GroupThreads(4)
    public boolean linkedProduce() {
        return linkedQueue.offer(1);
    }

    @Benchmark
    @Group("linkedBlockingQueue")
    @GroupThreads(4)
    public Integer linkedConsume() {
        return linkedQueue.poll();
    }
}
