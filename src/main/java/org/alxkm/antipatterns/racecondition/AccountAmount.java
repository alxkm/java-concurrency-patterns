package org.alxkm.antipatterns.racecondition;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class representing an account amount, contrasting an unsynchronized increment with three
 * correct alternatives: an intrinsic monitor, an explicit {@link Lock} and an {@link AtomicInteger}.
 * <p>
 * Each technique guards its own field. A single field cannot be shared between them, because a
 * {@code synchronized} block and a {@code ReentrantLock} are unrelated mechanisms: threads holding
 * different kinds of guard do not exclude one another, so mixing them on one field would leave the
 * race in place while looking synchronized.
 * <p>
 * Note that every getter is guarded the same way as its corresponding increment. Publishing a value
 * through an unguarded read would drop the visibility guarantee that the increment established, and
 * a reader could observe a stale amount no matter how carefully the write was synchronized.
 */
public class AccountAmount {
    private int unsafeAmount;

    /**
     * Guarded by {@code this}.
     */
    private int synchronizedAmount;

    private final Lock lock = new ReentrantLock();

    /**
     * Guarded by {@link #lock}.
     */
    private int lockAmount;

    private final AtomicInteger atomicAmount = new AtomicInteger();

    /**
     * Increments the amount without any synchronization.
     * <p>
     * {@code amount++} is a read-modify-write sequence, not a single operation, so two threads can
     * read the same value and write back the same result, losing one increment.
     */
    public void unsafeIncrementAmount() {
        unsafeAmount++;
    }

    /**
     * Retrieves the amount written by {@link #unsafeIncrementAmount()}.
     * <p>
     * This read is unsynchronized too, and so is not guaranteed to observe the writes of other
     * threads at all. It is only safe to call once the writing threads have been joined, which
     * establishes a happens-before edge.
     *
     * @return the current unsafe amount.
     */
    public int getUnsafeAmount() {
        return unsafeAmount;
    }

    /**
     * Increments the amount while holding this object's intrinsic monitor.
     */
    public synchronized void safeSynchronizedIncrementAmount() {
        synchronizedAmount++;
    }

    /**
     * Retrieves the amount guarded by this object's intrinsic monitor.
     *
     * @return the current synchronized amount.
     */
    public synchronized int getSynchronizedAmount() {
        return synchronizedAmount;
    }

    /**
     * Increments the amount while holding an explicit {@link ReentrantLock}.
     * <p>
     * {@code lock()} blocks and parks the thread until the lock is available. Spinning on
     * {@code tryLock()} in a loop would reach the same result while burning a CPU core, which is the
     * busy-waiting antipattern.
     */
    public void safeLockIncrementAmount() {
        lock.lock();
        try {
            lockAmount++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves the amount guarded by the explicit lock.
     *
     * @return the current lock-guarded amount.
     */
    public int getLockAmount() {
        lock.lock();
        try {
            return lockAmount;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Increments the amount using a single atomic compare-and-swap, without blocking.
     */
    public void incrementAtomicAmount() {
        atomicAmount.incrementAndGet();
    }

    /**
     * Retrieves the atomically updated amount.
     *
     * @return the current atomic amount.
     */
    public int getAtomicAmount() {
        return atomicAmount.get();
    }
}
