package org.alxkm.memorymodel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The happens-before edges the platform gives you, and what each one is worth.
 * <p>
 * {@link VisibilityExample} shows what goes wrong when there is no edge between a write and a read.
 * This class shows the four places an edge appears without any special effort, so you can recognise
 * when you already have one and stop reaching for {@code volatile} out of superstition.
 * <p>
 * Every method here carries a <em>guarantee</em>, not a probability: each returns the value that the
 * memory model requires, on every run, on every JVM. That is what makes them worth asserting on.
 *
 * <h2>The edges</h2>
 * <ol>
 *   <li><b>Thread.start()</b> -- everything the starting thread did before {@code start()}
 *       happens-before the first action of the new thread.</li>
 *   <li><b>Thread.join()</b> -- everything the joined thread did happens-before {@code join()} returns.
 *       This is the edge that makes the tests in this repository able to read a counter after joining
 *       its writers without any synchronisation of their own.</li>
 *   <li><b>volatile write/read</b> -- a volatile write happens-before every later volatile read of the
 *       same field. Crucially the edge carries <em>everything</em> written before it, not just the
 *       volatile field, which is what makes the flag-plus-payload idiom work.</li>
 *   <li><b>lock release/acquire</b> -- unlocking a monitor happens-before any later locking of the same
 *       monitor. A different lock gives you nothing, which is why mixing {@code synchronized} and a
 *       {@link Lock} over one field does not work.</li>
 * </ol>
 */
public final class HappensBeforeExample {

    private int plainPayload;
    private volatile boolean ready;

    private final Lock lock = new ReentrantLock();
    private int lockGuarded;

    /**
     * Edge 1: writes made before {@code Thread.start()} are visible inside the new thread.
     * <p>
     * The payload is written to a plain field with no synchronisation at all, and the new thread still
     * must see it.
     *
     * @param value the value to publish through the start edge.
     * @return the value the started thread read back.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedByThreadStart(int value) throws InterruptedException {
        plainPayload = value;

        AtomicInteger seen = new AtomicInteger();
        Thread thread = new Thread(() -> seen.set(plainPayload), "start-edge");
        thread.start(); // everything above happens-before the thread's first action
        thread.join();

        return seen.get();
    }

    /**
     * Edge 2: writes made inside a thread are visible after {@code Thread.join()} returns.
     *
     * @param value the value the spawned thread writes to a plain field.
     * @return the value this thread reads back after joining.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedByThreadJoin(int value) throws InterruptedException {
        Thread thread = new Thread(() -> plainPayload = value, "join-edge");
        thread.start();
        thread.join(); // everything the thread did happens-before this returns

        return plainPayload;
    }

    /**
     * Edge 3: a volatile write publishes everything written before it, not just itself.
     * <p>
     * This is the idiom behind every "flag plus payload" handover, and behind double-checked locking.
     * The writer fills in a plain field and then flips a volatile flag; a reader that sees the flag set
     * is guaranteed to see the payload too. Take {@code volatile} off {@code ready} and the guarantee
     * disappears -- the reader could see the flag without the payload.
     *
     * @param value the payload to hand over.
     * @return the payload the reader observed once it saw the flag.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedByVolatileFlag(int value) throws InterruptedException {
        AtomicInteger seen = new AtomicInteger(-1);

        Thread reader = new Thread(() -> {
            while (!ready) {
                Thread.onSpinWait();
            }
            seen.set(plainPayload); // guaranteed to be `value`, though the field is plain
        }, "volatile-reader");
        reader.start();

        plainPayload = value; // plain write ...
        ready = true;         // ... published by the volatile write that follows it

        reader.join();
        return seen.get();
    }

    /**
     * Edge 4: releasing a lock publishes to whoever acquires the same lock next.
     * <p>
     * "The same lock" is the load-bearing part. Two threads guarding one field with two different locks
     * exclude nobody and publish nothing to each other.
     *
     * @param value the value to write while holding the lock.
     * @return the value read back while holding the same lock.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedByLock(int value) throws InterruptedException {
        Thread writer = new Thread(() -> {
            lock.lock();
            try {
                lockGuarded = value;
            } finally {
                lock.unlock(); // happens-before the next acquisition of this lock
            }
        }, "lock-writer");
        writer.start();
        writer.join();

        lock.lock();
        try {
            return lockGuarded;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs each edge once and prints what came back.
     *
     * @param args command line arguments (not used).
     * @throws InterruptedException if the demo is interrupted.
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Thread.start()  published: " + new HappensBeforeExample().publishedByThreadStart(1));
        System.out.println("Thread.join()   published: " + new HappensBeforeExample().publishedByThreadJoin(2));
        System.out.println("volatile flag   published: " + new HappensBeforeExample().publishedByVolatileFlag(3));
        System.out.println("lock release    published: " + new HappensBeforeExample().publishedByLock(4));
    }
}
