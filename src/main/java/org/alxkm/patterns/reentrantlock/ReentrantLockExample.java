package org.alxkm.patterns.reentrantlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * What ReentrantLock can do that synchronized cannot: give up, and be interrupted while waiting.
 * <p>
 * A transfer between two accounts has to hold both accounts' locks. If one thread moves money from A
 * to B while another moves money from B to A, and each takes the lock of the account it is paying from
 * first, each ends up holding one lock and waiting for the other. With synchronized, or with lock(),
 * that wait never ends: {@link #transferInCallerOrder} shows it, and the JVM reports the deadlock.
 * <p>
 * Two ways out:
 * <ul>
 *   <li><b>One global order.</b> Always lock the account with the smaller id first. This works with
 *   synchronized too, and it is the better fix when every caller can follow it.</li>
 *   <li><b>Back off.</b> {@link #transfer} takes the first lock, then only tries the second with
 *   tryLock(). If that fails it lets go of the first as well, pauses for a random moment, and starts
 *   again, until a deadline. It cannot deadlock: nobody waits while holding a lock. The random pause
 *   matters: two threads that back off for the same time retry at the same time and can fail together
 *   again and again (a livelock).</li>
 * </ul>
 * Separately, lockInterruptibly() waits for the lock but throws InterruptedException if the thread is
 * interrupted meanwhile; lock(), like synchronized, ignores the interrupt and goes on waiting. See
 * {@link Account#balanceInterruptibly()}.
 */
public final class ReentrantLockExample {

    private ReentrantLockExample() {
    }

    /**
     * A balance guarded by its own lock.
     */
    public static final class Account {
        private final ReentrantLock lock = new ReentrantLock();
        private final String name;
        private long balance;

        public Account(String name, long balance) {
            this.name = name;
            this.balance = balance;
        }

        public long balance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Reads the balance, but gives up waiting for the lock if this thread is interrupted.
         *
         * @return the balance.
         * @throws InterruptedException if interrupted while waiting for the lock.
         */
        public long balanceInterruptibly() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }

        ReentrantLock lock() {
            return lock;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Moves money from one account to the other, backing off instead of waiting while it holds a lock.
     *
     * @param from    the account to take the money from.
     * @param to      the account to pay it into.
     * @param amount  how much.
     * @param timeout how long to keep trying.
     * @param unit    the unit of {@code timeout}.
     * @return true if the money was moved, false if both locks could not be had before the timeout.
     * @throws InterruptedException  if interrupted between attempts.
     * @throws IllegalStateException if {@code from} does not have {@code amount}.
     */
    public static boolean transfer(Account from, Account to, long amount, long timeout, TimeUnit unit)
            throws InterruptedException {
        return transfer(from, to, amount, timeout, unit, () -> { });
    }

    /**
     * {@link #transfer(Account, Account, long, long, TimeUnit)} with an action that runs once, after the
     * first lock is taken and before the second is tried. A test uses it to make two transfers hold one
     * lock each at the same moment, which with real threads happens only now and then.
     */
    static boolean transfer(Account from, Account to, long amount, long timeout, TimeUnit unit,
                            Runnable afterFirstLock) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        Runnable pause = afterFirstLock;
        while (true) {
            from.lock.lock();
            try {
                pause.run();
                pause = () -> { };
                if (to.lock.tryLock()) {
                    try {
                        move(from, to, amount);
                        return true;
                    } finally {
                        to.lock.unlock();
                    }
                }
            } finally {
                from.lock.unlock();
            }
            // Holding nothing now. Wait a random moment, so two threads that both failed do not retry in step.
            if (System.nanoTime() >= deadline) {
                return false;
            }
            LockSupport.parkNanos(ThreadLocalRandom.current().nextLong(10_000, 1_000_000));
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    /**
     * The transfer that deadlocks: the payer's lock first, then the payee's, both with lock().
     */
    static void transferInCallerOrder(Account from, Account to, long amount, Runnable afterFirstLock) {
        from.lock.lock();
        try {
            afterFirstLock.run();
            to.lock.lock();
            try {
                move(from, to, amount);
            } finally {
                to.lock.unlock();
            }
        } finally {
            from.lock.unlock();
        }
    }

    private static void move(Account from, Account to, long amount) {
        if (from.balance < amount) {
            throw new IllegalStateException(from + " has " + from.balance + ", cannot pay " + amount);
        }
        from.balance -= amount;
        to.balance += amount;
    }

    /**
     * Runs two opposite transfers with {@link #transferInCallerOrder}, each pausing once it holds its
     * first lock until the other holds its own, and asks the JVM which threads are deadlocked.
     *
     * @return the names of the two transfer threads the JVM found deadlocked; both, every time. The
     * threads are daemons and stay blocked for good.
     * @throws InterruptedException if interrupted while looking.
     */
    public static List<String> deadlockedOppositeTransfers() throws InterruptedException {
        Account a = new Account("A", 100);
        Account b = new Account("B", 100);
        CyclicBarrier bothHoldOneLock = new CyclicBarrier(2);
        Thread aToB = new Thread(() -> transferInCallerOrder(a, b, 10, () -> meet(bothHoldOneLock)), "A-to-B");
        Thread bToA = new Thread(() -> transferInCallerOrder(b, a, 10, () -> meet(bothHoldOneLock)), "B-to-A");
        aToB.setDaemon(true);
        bToA.setDaemon(true);
        aToB.start();
        bToA.start();

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            List<String> found = new ArrayList<>();
            long[] deadlocked = threads.findDeadlockedThreads(); // sees ReentrantLock as well as monitors
            if (deadlocked != null) {
                for (long id : deadlocked) {
                    if (id == aToB.threadId() || id == bToA.threadId()) {
                        found.add(id == aToB.threadId() ? aToB.getName() : bToA.getName());
                    }
                }
            }
            if (found.size() == 2) {
                found.sort(null);
                return found;
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }
        return List.of();
    }

    private static void meet(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Opposite transfers with lock(), payer first. Deadlocked: " + deadlockedOppositeTransfers());

        // Enough on each side for the worst order: every transfer one way before any the other way.
        Account a = new Account("A", 10_000);
        Account b = new Account("B", 10_000);
        Thread aToB = new Thread(() -> repeat(a, b), "A-to-B");
        Thread bToA = new Thread(() -> repeat(b, a), "B-to-A");
        aToB.start();
        bToA.start();
        aToB.join();
        bToA.join();
        System.out.println("10,000 transfers each way with tryLock and back-off: A=" + a.balance()
                + ", B=" + b.balance() + ", total " + (a.balance() + b.balance()));
    }

    private static void repeat(Account from, Account to) {
        try {
            for (int i = 0; i < 10_000; i++) {
                if (!transfer(from, to, 1, 10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("gave up on a transfer");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
