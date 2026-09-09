package org.alxkm.memorymodel;

import java.util.concurrent.TimeUnit;

/**
 * A write one thread makes that another thread never sees, and the one-word change that fixes it.
 * <p>
 * A reader spins until a flag turns true. A writer sets it a moment later. With a plain field the
 * reader typically spins forever; with a {@code volatile} field it stops immediately.
 *
 * <h2>Why the plain field hangs</h2>
 * Nothing in the loop below writes to {@code plainFlag}, so the JIT is entitled to assume it cannot
 * change and hoist the read out of the loop -- turning {@code while (!plainFlag)} into
 * {@code if (!plainFlag) while (true)}. That is a legal transformation precisely because there is no
 * happens-before edge between the writer's store and the reader's load. Without such an edge the
 * reader is under no obligation ever to observe the write, so the compiler may plan for it never
 * happening. Store buffers and per-core caches can produce the same outcome even without the hoist.
 *
 * <h2>Why volatile fixes it</h2>
 * A volatile write happens-before every subsequent volatile read of the same field. That edge forbids
 * both the hoist and the stale read, so the reader is guaranteed to terminate.
 * <p>
 * Note what volatile does <em>not</em> do: it makes the write visible, it does not make a
 * read-modify-write atomic. {@code volatileCounter++} still loses updates. Visibility and atomicity are
 * separate problems -- see {@code org.alxkm.antipatterns.nonatomiccompoundactions} for the second one.
 */
public final class VisibilityExample {

    /** How long to let the writer's value propagate before giving up on the reader. */
    private static final long DEFAULT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(2);

    /** Long enough for the reader thread to enter its spin loop and for the JIT to compile it. */
    private static final long WRITE_DELAY_MILLIS = 300;

    private boolean plainFlag;
    private volatile boolean volatileFlag;

    /**
     * Runs the reader against a plain, non-volatile field.
     * <p>
     * There is no guarantee either way here. The JMM permits the reader to miss the write forever, and
     * on a JIT-compiled JVM that is the usual outcome; it is also free to observe it. The method
     * reports which happened rather than asserting one, because the interesting fact is that the
     * outcome is not specified.
     *
     * @return true if the reader observed the write before the timeout.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public boolean plainReaderObservesWrite() throws InterruptedException {
        Thread reader = startDaemon("plain-reader", () -> {
            long spins = 0;
            while (!plainFlag) {
                spins++;
            }
            return spins;
        });

        Thread.sleep(WRITE_DELAY_MILLIS);
        plainFlag = true;

        reader.join(DEFAULT_TIMEOUT_MILLIS);
        boolean observed = !reader.isAlive();
        plainFlag = true; // let the reader out if it is somehow still spinning
        return observed;
    }

    /**
     * Runs the same reader against a volatile field.
     * <p>
     * Unlike the plain case this outcome <em>is</em> specified: the volatile write happens-before the
     * reader's next volatile read, so the reader must terminate.
     *
     * @return true if the reader observed the write before the timeout, which it always should.
     * @throws InterruptedException if this thread is interrupted while waiting.
     */
    public boolean volatileReaderObservesWrite() throws InterruptedException {
        Thread reader = startDaemon("volatile-reader", () -> {
            long spins = 0;
            while (!volatileFlag) {
                spins++;
            }
            return spins;
        });

        Thread.sleep(WRITE_DELAY_MILLIS);
        volatileFlag = true;

        reader.join(DEFAULT_TIMEOUT_MILLIS);
        return !reader.isAlive();
    }

    /**
     * Starts a daemon thread running the given spin loop.
     * <p>
     * Daemon so that a reader left spinning -- the whole point of the plain case -- cannot keep the JVM
     * alive after the demo or the test that ran it has finished.
     *
     * @param name the thread name.
     * @param loop the spin loop to run; its result is discarded but keeps the loop from being optimised
     *             away entirely.
     * @return the started thread.
     */
    private static Thread startDaemon(String name, SpinLoop loop) {
        Thread thread = new Thread(loop::spin, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** A spin loop returning the number of iterations it took, so the loop body is not dead code. */
    @FunctionalInterface
    private interface SpinLoop {
        long spin();
    }

    /**
     * Runs both variants and prints what this JVM did.
     *
     * @param args command line arguments (not used).
     * @throws InterruptedException if the demo is interrupted.
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("plain field    -> reader observed the write: "
                + new VisibilityExample().plainReaderObservesWrite());
        System.out.println("volatile field -> reader observed the write: "
                + new VisibilityExample().volatileReaderObservesWrite());
    }
}
