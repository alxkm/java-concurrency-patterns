package org.alxkm.memorymodel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handing a freshly built object to another thread without it observing the object half-built.
 * <p>
 * Publication is a separate problem from mutation. An object can be perfectly immutable in intent and
 * still be broken if the reference to it escapes without an ordering edge: another thread may see the
 * reference before it sees the constructor's writes, and read a field that is still at its default.
 *
 * <h2>Why the unsafe version can fail</h2>
 * {@code holder = new Holder(42)} is not one action. It allocates, writes the field, and assigns the
 * reference -- and without an edge the assignment may become visible to another thread before the field
 * write does. A reader then sees a non-null {@code Holder} whose {@code value} is 0. The reader is not
 * doing anything wrong and the object is never mutated afterwards; the bug is entirely in the handover.
 * <p>
 * This is the same failure that makes unsynchronised double-checked locking broken, which is why the
 * fix there is the same as the fix here.
 *
 * <h2>The safe options</h2>
 * Each of the methods below publishes correctly, and each relies on a different guarantee:
 * <ul>
 *   <li><b>final field</b> -- the JMM makes a constructor's writes to final fields visible to any thread
 *       that sees the reference, with no synchronisation at the handover at all. The cheapest option,
 *       and the reason immutable classes are easy to share.</li>
 *   <li><b>volatile field</b> -- the write publishes everything before it, as in
 *       {@link HappensBeforeExample#publishedByVolatileFlag(int)}.</li>
 *   <li><b>AtomicReference</b> -- the same edge, with compare-and-set available on top.</li>
 *   <li><b>static initialiser</b> -- the JVM's class initialisation lock provides the edge, which is
 *       what makes the holder idiom a correct lazy singleton.</li>
 * </ul>
 */
public final class SafePublicationExample {

    /** Published without any ordering edge: a reader may see the reference before the field write. */
    private Holder unsafe;

    /** The volatile write publishes the constructor's writes along with the reference. */
    private volatile Holder viaVolatile;

    private final AtomicReference<Holder> viaAtomic = new AtomicReference<>();

    /** A mutable holder: nothing here is final, so only the handover can provide the guarantee. */
    static final class Holder {
        private int value;

        Holder(int value) {
            this.value = value;
        }

        int value() {
            return value;
        }
    }

    /** An immutable holder: the final field carries its own publication guarantee. */
    static final class FinalHolder {
        private final int value;

        FinalHolder(int value) {
            this.value = value;
        }

        int value() {
            return value;
        }
    }

    /** Initialised by the class initialiser, whose lock provides the edge for every reader. */
    private static final class StaticHolder {
        static final Holder INSTANCE = new Holder(STATIC_VALUE);
    }

    private static final int STATIC_VALUE = 99;

    /**
     * Publishes through a plain field, which provides no ordering edge.
     * <p>
     * A single run almost always reads the right value -- the window is a few instructions wide -- so
     * this method is a demonstration of the shape of the bug, not a reliable reproducer. Catching it
     * takes jcstress; see the {@code src/jcstress} source set.
     *
     * @param value the value to publish.
     * @return the value the reader observed, which is not guaranteed to be {@code value}.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedUnsafely(int value) throws InterruptedException {
        AtomicInteger seen = new AtomicInteger(-1);
        Thread reader = new Thread(() -> {
            Holder local;
            do {
                local = unsafe;
            } while (local == null);
            seen.set(local.value());
        }, "unsafe-reader");
        reader.start();

        unsafe = new Holder(value);

        reader.join();
        return seen.get();
    }

    /**
     * Publishes through a final field, which needs no synchronisation at the handover.
     *
     * @param value the value to publish.
     * @return the value the reader observed; guaranteed to equal {@code value}.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedByFinalField(int value) throws InterruptedException {
        AtomicReference<FinalHolder> box = new AtomicReference<>();
        AtomicInteger seen = new AtomicInteger(-1);

        Thread reader = new Thread(() -> {
            FinalHolder local;
            do {
                local = box.get();
            } while (local == null);
            seen.set(local.value());
        }, "final-reader");
        reader.start();

        box.set(new FinalHolder(value));

        reader.join();
        return seen.get();
    }

    /**
     * Publishes through a volatile field.
     *
     * @param value the value to publish.
     * @return the value the reader observed; guaranteed to equal {@code value}.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedByVolatileField(int value) throws InterruptedException {
        AtomicInteger seen = new AtomicInteger(-1);
        Thread reader = new Thread(() -> {
            Holder local;
            do {
                local = viaVolatile;
            } while (local == null);
            seen.set(local.value());
        }, "volatile-reader");
        reader.start();

        viaVolatile = new Holder(value);

        reader.join();
        return seen.get();
    }

    /**
     * Publishes through an {@link AtomicReference}.
     *
     * @param value the value to publish.
     * @return the value the reader observed; guaranteed to equal {@code value}.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedByAtomicReference(int value) throws InterruptedException {
        AtomicInteger seen = new AtomicInteger(-1);
        Thread reader = new Thread(() -> {
            Holder local;
            do {
                local = viaAtomic.get();
            } while (local == null);
            seen.set(local.value());
        }, "atomic-reader");
        reader.start();

        viaAtomic.set(new Holder(value));

        reader.join();
        return seen.get();
    }

    /**
     * Reads an instance published by a class initialiser, from a thread that never synchronises.
     *
     * @return the statically published value; guaranteed to be {@link #STATIC_VALUE}.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    public int publishedByStaticInitialiser() throws InterruptedException {
        AtomicInteger seen = new AtomicInteger(-1);
        Thread reader = new Thread(() -> seen.set(StaticHolder.INSTANCE.value()), "static-reader");
        reader.start();
        reader.join();
        return seen.get();
    }

    /**
     * Runs each publication idiom once and prints what the reader saw.
     *
     * @param args command line arguments (not used).
     * @throws InterruptedException if the demo is interrupted.
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("unsafe plain field : " + new SafePublicationExample().publishedUnsafely(42));
        System.out.println("final field        : " + new SafePublicationExample().publishedByFinalField(42));
        System.out.println("volatile field     : " + new SafePublicationExample().publishedByVolatileField(42));
        System.out.println("AtomicReference    : " + new SafePublicationExample().publishedByAtomicReference(42));
        System.out.println("static initialiser : " + new SafePublicationExample().publishedByStaticInitialiser());
    }
}
