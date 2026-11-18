package org.alxkm.patterns.locks;

import java.util.concurrent.locks.LockSupport;

/**
 * LockSupport: park and unpark, the primitive under every lock and synchronizer in
 * java.util.concurrent.
 * <p>
 * Each thread has one permit, either available or not. unpark(t) makes t's permit available.
 * park() takes the permit and returns at once if it is there, and otherwise blocks until it is.
 * Two consequences:
 * <ul>
 *   <li>An unpark that comes before the park is not lost. This is the difference from wait/notify,
 *   where a notify with nobody waiting does nothing and a later wait() sleeps through it.</li>
 *   <li>Permits do not add up. Two unparks before a park leave one permit, not two.</li>
 * </ul>
 * park() may also return for no reason at all (a spurious wakeup), so it is always called in a loop
 * that rechecks a condition. {@link OneShotSignal} shows the whole pattern: a volatile flag for the
 * condition, park in a loop, unpark to wake.
 */
public final class LockSupportExample {

    private LockSupportExample() {
    }

    /**
     * Lets one thread wait until another signals it. Built the way the JDK's own synchronizers use
     * park and unpark, without the queue: one waiter at most.
     */
    public static final class OneShotSignal {
        private volatile boolean signalled;
        private volatile Thread waiter;

        /**
         * Waits until {@link #signal()} has been called; returns at once if it already has.
         *
         * @throws InterruptedException if the thread is interrupted while waiting. unpark is also how
         *                              interrupt wakes a parked thread, so park returns and the loop
         *                              checks for it.
         */
        public void await() throws InterruptedException {
            waiter = Thread.currentThread();
            // The order matters. This thread writes waiter, then reads signalled; signal() writes
            // signalled, then reads waiter. Both fields are volatile, so at least one side sees the other's
            // write: either this loop sees the flag, or signal() sees this thread and unparks it.
            while (!signalled) {
                // The blocker shows up in a thread dump: "parking to wait for <OneShotSignal>".
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        }

        /**
         * Wakes the waiter, or lets a waiter that comes later return at once.
         */
        public void signal() {
            signalled = true;
            Thread current = waiter;
            if (current != null) {
                LockSupport.unpark(current);
            }
        }
    }

    /**
     * Unparks the current thread, then parks it. The permit is already there, so park returns at once.
     *
     * @return how long park took, in milliseconds; close to zero.
     */
    public static long parkAfterUnpark() {
        long start = System.nanoTime();
        LockSupport.unpark(Thread.currentThread());
        LockSupport.park();
        return (System.nanoTime() - start) / 1_000_000;
    }

    /**
     * The same order with wait/notify: notifyAll first, then wait. It reached nobody, so the wait
     * sleeps until its timeout.
     *
     * @param timeoutMillis how long to wait.
     * @return how long wait took, in milliseconds; usually the whole timeout.
     * @throws InterruptedException if interrupted while waiting.
     */
    public static long waitAfterNotify(long timeoutMillis) throws InterruptedException {
        Object monitor = new Object();
        long start = System.nanoTime();
        synchronized (monitor) {
            monitor.notifyAll(); // nobody is waiting yet, so this wakes no one
        }
        synchronized (monitor) {
            // Not in a loop, on purpose: this shows the lost notify, not how to use wait.
            monitor.wait(timeoutMillis);
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("unpark, then park: park took " + parkAfterUnpark() + " ms");
        System.out.println("notify, then wait: wait took " + waitAfterNotify(500) + " ms (the notify was lost)");

        OneShotSignal signal = new OneShotSignal();
        Thread waiter = new Thread(() -> {
            try {
                signal.await();
                System.out.println(Thread.currentThread().getName() + " was signalled and continues");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "waiter");
        waiter.start();
        while (LockSupport.getBlocker(waiter) != signal) {
            Thread.onSpinWait();
        }
        System.out.println("waiter is parked on " + LockSupport.getBlocker(waiter).getClass().getSimpleName()
                + "; signalling it");
        signal.signal();
        waiter.join();
    }
}
