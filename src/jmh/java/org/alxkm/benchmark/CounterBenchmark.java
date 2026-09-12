package org.alxkm.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The four usual ways to increment a shared counter, measured.
 * <p>
 * This repository says atomics are "faster than synchronization with synchronized or volatile". That
 * is the kind of claim that is either true with conditions attached or not true at all, so it is worth
 * having numbers for it rather than repeating it.
 * <p>
 * Run it at two thread counts, because the answer is different:
 * <pre>
 *   ./gradlew jmh -PjmhArgs="CounterBenchmark -t 1"
 *   ./gradlew jmh -PjmhArgs="CounterBenchmark -t 8"
 * </pre>
 * Uncontended, the atomics are already ahead, and by more than the old folklore would predict. That
 * folklore assumed biased locking, which made an uncontended monitor nearly free; it was disabled in
 * JDK 15 and removed in 18, so on a current JVM an uncontended monitor pays a real CAS.
 * <p>
 * Contended is where they separate further, and where {@link LongAdder} pulls away from
 * {@link AtomicLong}: the adder spreads its state across padded cells so threads stop fighting over one
 * cache line, which is the effect {@code FalseSharingExample} measures directly.
 * <p>
 * Note what is not being measured. This is a pure write workload with no read of the total. LongAdder
 * pays for its spread on {@code sum()}, which has to walk every cell, so a counter that is read as
 * often as it is written is a different question from this one.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class CounterBenchmark {

    private final Object monitor = new Object();
    private long monitorCounter;

    private final ReentrantLock lock = new ReentrantLock();
    private long lockCounter;

    private final AtomicLong atomicCounter = new AtomicLong();
    private final LongAdder adderCounter = new LongAdder();

    @Benchmark
    public long synchronizedIncrement() {
        synchronized (monitor) {
            return ++monitorCounter;
        }
    }

    @Benchmark
    public long reentrantLockIncrement() {
        lock.lock();
        try {
            return ++lockCounter;
        } finally {
            lock.unlock();
        }
    }

    @Benchmark
    public long atomicLongIncrement() {
        return atomicCounter.incrementAndGet();
    }

    /**
     * LongAdder has no incrementAndGet, which is the trade it makes: cheap writes, and a sum that has
     * to visit every cell. Returning the increment keeps the signature comparable without paying for a
     * sum on each call.
     *
     * @return a constant, so the benchmark measures the write and nothing else.
     */
    @Benchmark
    public long longAdderIncrement() {
        adderCounter.increment();
        return 1L;
    }
}
