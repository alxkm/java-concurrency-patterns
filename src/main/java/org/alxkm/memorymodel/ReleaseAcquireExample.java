package org.alxkm.memorymodel;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Release/acquire: the cheapest ordering that still publishes data, and the place where it stops being
 * enough.
 * <p>
 * A writer fills a plain field and then raises a flag with {@code setRelease}. A reader spins on the flag
 * with {@code getAcquire} and then reads the field. That is enough for the reader to see the payload: a
 * release write and an acquire read that observes it order everything before the write ahead of
 * everything after the read, which is the same edge a volatile pair gives for a one-way handover.
 *
 * <h2>Why not just volatile</h2>
 * Volatile is release/acquire plus one more guarantee: every volatile access in the program falls into a
 * single total order. That order is what the StoreLoad barrier after a volatile write pays for, and it is
 * the expensive barrier. A handover in one direction never needs it, which is why the JDK's own queues and
 * futures publish with release and read with acquire wherever they can.
 *
 * <h2>Where it is not enough</h2>
 * Dekker's idiom: each thread writes its own flag, then reads the other's. With release/acquire both
 * threads can read the other's flag as still unset, because nothing orders a store before a later load of
 * a different variable. That StoreLoad reordering is exactly what volatile forbids and release/acquire
 * permits. {@code ReleaseAcquireStressTest} in the jcstress source set shows both halves: the handover is
 * never seen half done, and the Dekker outcome turns up.
 * <p>
 * On x86 a release store and an acquire load are plain {@code mov} instructions, so the mode costs nothing
 * there beyond restraining the JIT. The saving is real on ARM, where a volatile needs heavier barriers.
 */
public final class ReleaseAcquireExample {

    private static final VarHandle READY;

    static {
        try {
            READY = MethodHandles.lookup().findVarHandle(ReleaseAcquireExample.class, "ready", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Both plain on purpose. The payload needs no ordering of its own, since the flag carries its
    // visibility, and the flag is only ever touched through READY.
    private int payload;
    private boolean ready;

    /**
     * Hands a value from this thread to a reader thread through a release/acquire flag. One handover per
     * instance: the flag is never lowered again.
     *
     * @param value the value to hand over.
     * @return what the reader found in the payload once it saw the flag; guaranteed to equal {@code value}.
     * @throws InterruptedException if this thread is interrupted while joining the reader.
     */
    public int handOver(int value) throws InterruptedException {
        AtomicInteger seen = new AtomicInteger(-1);
        Thread reader = new Thread(() -> {
            while (!(boolean) READY.getAcquire(this)) {
                Thread.onSpinWait();
            }
            seen.set(payload);
        }, "acquire-reader");
        reader.start();

        payload = value;                // a plain write...
        READY.setRelease(this, true);   // ...published by the release that follows it

        reader.join();
        return seen.get();
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("reader saw payload: " + new ReleaseAcquireExample().handOver(42));
        System.out.println("For the half release/acquire does not cover, run:");
        System.out.println("    ./gradlew jcstress -PjcstressArgs=\"-t ReleaseAcquire -m quick\"");
    }
}
