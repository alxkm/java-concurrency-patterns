package org.alxkm.patterns.atomics;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Counts the work in flight and remembers the peak, using two plain {@code long} fields and a
 * {@link VarHandle} for each.
 * <p>
 * An {@code AtomicLong} is one field plus the machinery to update it atomically. A handle is that
 * machinery without the wrapper, and several of the {@code Atomic*} classes are built on one. Holding the
 * field directly buys two things. It saves an object per counter, which matters when the counter lives in
 * something there are millions of, such as a cache entry or a connection. And it puts the access mode in
 * your hands instead of always paying for a full volatile; see
 * {@link org.alxkm.memorymodel.ReleaseAcquireExample} for what the weaker modes are for.
 *
 * <h2>Two kinds of atomic update</h2>
 * The in-flight count is a read-modify-write the hardware can do in one instruction, so
 * {@code getAndAdd} compiles to a single {@code lock xadd} on x86 and never retries.
 * <p>
 * The peak has no such instruction. "Set it to x if x is larger" is a decision, and a decision made on a
 * value another thread may have changed in the meantime has to be checked and retried. That is the
 * compare-and-set loop every lock-free structure is built from, and the same loop
 * {@code AtomicLong.accumulateAndGet} runs for you.
 *
 * <h2>Against the field updaters</h2>
 * {@link java.util.concurrent.atomic.AtomicLongFieldUpdater} solved the same problem before Java 9, by
 * reflection when it is created and a check of the receiver's type on every call. A {@code VarHandle} held
 * in a {@code static final} field is inlined by the JIT down to the bare instruction, and it has the
 * acquire, release and opaque modes the updaters never had.
 */
public final class VarHandleGauge {

    private static final VarHandle IN_FLIGHT;
    private static final VarHandle PEAK;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            IN_FLIGHT = lookup.findVarHandle(VarHandleGauge.class, "inFlight", long.class);
            PEAK = lookup.findVarHandle(VarHandleGauge.class, "peak", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Volatile, so the getters below are ordinary volatile reads. The handles are only needed for the
    // atomic updates, which a volatile field cannot do on its own: inFlight++ would still lose updates.
    private volatile long inFlight;
    private volatile long peak;

    /** Records that one more piece of work has started, and raises the peak if this is a new high. */
    public void enter() {
        long now = (long) IN_FLIGHT.getAndAdd(this, 1L) + 1;
        raisePeakTo(now);
    }

    /** Records that one piece of work has finished. */
    public void exit() {
        IN_FLIGHT.getAndAdd(this, -1L);
    }

    /** @return how much work is in flight right now. */
    public long inFlight() {
        return inFlight;
    }

    /** @return the most work that was ever in flight at once. */
    public long peak() {
        return peak;
    }

    private void raisePeakTo(long candidate) {
        long current = peak;
        while (candidate > current) {
            // The weak form may fail even when the value matched. Inside a loop that retries anyway that
            // costs nothing, and on ARM, where a CAS is a load-linked/store-conditional pair, it is
            // cheaper than the strong form. Its memory effects are still those of a volatile access.
            if (PEAK.weakCompareAndSet(this, current, candidate)) {
                return;
            }
            current = peak;
        }
    }

    /**
     * Starts {@code workers} tasks that each stay in flight until every one of them has started.
     * <p>
     * Holding them all inside at once makes the peak a known number rather than an accident of
     * scheduling: it must come out as exactly {@code workers}, and in flight must come back to zero.
     *
     * @param workers how many tasks to run.
     * @return the gauge after every task has finished.
     */
    public static VarHandleGauge measure(int workers) {
        VarHandleGauge gauge = new VarHandleGauge();
        CountDownLatch allInside = new CountDownLatch(workers);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < workers; i++) {
                executor.execute(() -> {
                    gauge.enter();
                    try {
                        allInside.countDown();
                        // Nobody leaves until everybody has arrived. The timeout turns a stuck run into
                        // a peak below workers, which a test reports, instead of a hang.
                        allInside.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        gauge.exit();
                    }
                });
            }
        } // close() waits for every task to finish
        return gauge;
    }

    public static void main(String[] args) {
        VarHandleGauge gauge = measure(1_000);
        System.out.println("peak in flight : " + gauge.peak() + " (1000 tasks held until all had started)");
        System.out.println("in flight now  : " + gauge.inFlight());
    }
}
