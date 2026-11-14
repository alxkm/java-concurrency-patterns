package org.alxkm.patterns.locks;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;

/**
 * AbstractOwnableSynchronizer: one field, the thread that owns the synchronizer, and why it is worth a
 * base class.
 * <p>
 * AbstractQueuedSynchronizer extends it, which is how ReentrantLock knows its owner. But the field has
 * a second reader: the JVM. When a thread dump or ThreadMXBean asks what a thread holds, the JVM looks
 * for every AbstractOwnableSynchronizer whose owner is that thread and lists it under "Locked ownable
 * synchronizers". A home-made lock that keeps its owner in a field of its own is invisible there, and
 * the dump of a stuck application does not say who holds it.
 * <p>
 * The example is a spin lock written twice: once on top of AbstractOwnableSynchronizer, once not. They
 * behave the same; only the first shows up in a dump.
 */
public final class AbstractOwnableSynchronizerExample {

    private AbstractOwnableSynchronizerExample() {
    }

    /**
     * A non-reentrant spin lock. A waiting thread burns CPU instead of parking, which only pays off when
     * the lock is held for a few instructions; it keeps the example about ownership rather than queues.
     */
    public interface SpinLock {
        boolean tryLock();

        default void lock() {
            while (!tryLock()) {
                Thread.onSpinWait();
            }
        }

        /**
         * @throws IllegalMonitorStateException if the calling thread does not hold the lock.
         */
        void unlock();

        boolean isHeldByCurrentThread();
    }

    /**
     * Keeps its owner in AbstractOwnableSynchronizer, so thread dumps and ThreadMXBean can see it.
     */
    public static final class OwnableSpinLock extends AbstractOwnableSynchronizer implements SpinLock {
        private static final long serialVersionUID = 1L;

        private final AtomicBoolean locked = new AtomicBoolean();

        @Override
        public boolean tryLock() {
            if (locked.compareAndSet(false, true)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        public void unlock() {
            if (getExclusiveOwnerThread() != Thread.currentThread()) {
                throw new IllegalMonitorStateException("the lock is not held by " + Thread.currentThread());
            }
            setExclusiveOwnerThread(null);
            locked.set(false);
        }

        /**
         * The owner field is not volatile, and for this question it does not need to be. Another thread
         * may read a stale owner, but the only stale value that could equal the current thread is one this
         * thread wrote itself, and a thread always sees its own later writes. So "is it me?" is always
         * answered correctly; "who is it?" from another thread is only a hint, which is all a dump needs.
         */
        @Override
        public boolean isHeldByCurrentThread() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    /**
     * The same lock with the owner in its own field. Works the same, but nothing outside knows the field
     * is an owner.
     */
    public static final class PlainSpinLock implements SpinLock {
        private final AtomicBoolean locked = new AtomicBoolean();
        private Thread owner;

        @Override
        public boolean tryLock() {
            if (locked.compareAndSet(false, true)) {
                owner = Thread.currentThread();
                return true;
            }
            return false;
        }

        @Override
        public void unlock() {
            if (owner != Thread.currentThread()) {
                throw new IllegalMonitorStateException("the lock is not held by " + Thread.currentThread());
            }
            owner = null;
            locked.set(false);
        }

        @Override
        public boolean isHeldByCurrentThread() {
            return owner == Thread.currentThread();
        }
    }

    /**
     * Asks the JVM, the way a thread dump does, whether the thread is listed as holding the lock.
     *
     * @param thread a live thread.
     * @param lock   the lock to look for.
     * @return true if the lock is among the thread's locked ownable synchronizers.
     */
    public static boolean dumpShowsHolding(Thread thread, Object lock) {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = threads.getThreadInfo(new long[]{thread.threadId()}, false, true);
        if (infos[0] == null) {
            return false;
        }
        int identity = System.identityHashCode(lock);
        LockInfo[] held = infos[0].getLockedSynchronizers();
        return Arrays.stream(held).anyMatch(info -> info.getIdentityHashCode() == identity);
    }

    /**
     * Has a thread take the lock and keep it, and reports what a thread dump says it holds.
     *
     * @param lock the lock to take.
     * @return whether the dump lists the lock as held by that thread.
     * @throws InterruptedException if interrupted while waiting for the holder.
     */
    public static boolean visibleInThreadDump(SpinLock lock) throws InterruptedException {
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                held.countDown();
                done.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }, "lock-holder");
        holder.start();
        try {
            held.await();
            return dumpShowsHolding(holder, lock);
        } finally {
            done.countDown();
            holder.join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("A thread holds each lock. Does a thread dump say so?");
        System.out.println("  spin lock on AbstractOwnableSynchronizer: " + visibleInThreadDump(new OwnableSpinLock()));
        System.out.println("  spin lock with its own owner field:       " + visibleInThreadDump(new PlainSpinLock()));
    }
}
