package org.alxkm.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * CopyOnWriteArrayList against a synchronized list, at the read/write ratios that decide between them.
 * <p>
 * The README says CopyOnWrite collections are "particularly useful when write operations are
 * infrequent". True, and the interesting part is where infrequent stops being infrequent. Every write
 * copies the whole backing array, so the cost of a write grows with the size of the list while reads
 * take no lock at all.
 * <p>
 * The groups below fix the ratio by thread count: seven readers to one writer in the read-heavy case,
 * and an even split in the mixed case. JMH reports each group member separately as well as the group
 * total, so the read and write costs can be read off individually.
 * <pre>
 *   ./gradlew jmh -PjmhArgs="ListBenchmark"
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Group)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class ListBenchmark {

    /**
     * List size. Every CopyOnWrite write copies the whole backing array, so this is the axis that
     * decides between the two: reads stay lock free whatever the size, writes get linearly worse.
     */
    @Param({"1000", "10000", "100000"})
    public int size;

    private List<Integer> copyOnWrite;
    private List<Integer> synchronizedList;

    @Setup
    public void setUp() {
        List<Integer> seed = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            seed.add(i);
        }
        copyOnWrite = new CopyOnWriteArrayList<>(seed);
        synchronizedList = Collections.synchronizedList(new ArrayList<>(seed));
    }

    // Read heavy: 7 readers, 1 writer. The case CopyOnWrite is built for.

    @Benchmark
    @Group("copyOnWriteReadHeavy")
    @GroupThreads(7)
    public Integer copyOnWriteRead() {
        return copyOnWrite.get(ThreadLocalRandom.current().nextInt(size));
    }

    @Benchmark
    @Group("copyOnWriteReadHeavy")
    @GroupThreads(1)
    public void copyOnWriteWrite() {
        copyOnWrite.set(ThreadLocalRandom.current().nextInt(size), 1);
    }

    @Benchmark
    @Group("synchronizedReadHeavy")
    @GroupThreads(7)
    public Integer synchronizedRead() {
        return synchronizedList.get(ThreadLocalRandom.current().nextInt(size));
    }

    @Benchmark
    @Group("synchronizedReadHeavy")
    @GroupThreads(1)
    public void synchronizedWrite() {
        synchronizedList.set(ThreadLocalRandom.current().nextInt(size), 1);
    }

    // Even split: 4 readers, 4 writers. Where copying the array on every write stops paying.

    @Benchmark
    @Group("copyOnWriteMixed")
    @GroupThreads(4)
    public Integer copyOnWriteMixedRead() {
        return copyOnWrite.get(ThreadLocalRandom.current().nextInt(size));
    }

    @Benchmark
    @Group("copyOnWriteMixed")
    @GroupThreads(4)
    public void copyOnWriteMixedWrite() {
        copyOnWrite.set(ThreadLocalRandom.current().nextInt(size), 1);
    }

    @Benchmark
    @Group("synchronizedMixed")
    @GroupThreads(4)
    public Integer synchronizedMixedRead() {
        return synchronizedList.get(ThreadLocalRandom.current().nextInt(size));
    }

    @Benchmark
    @Group("synchronizedMixed")
    @GroupThreads(4)
    public void synchronizedMixedWrite() {
        synchronizedList.set(ThreadLocalRandom.current().nextInt(size), 1);
    }
}
