package org.alxkm.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Reading a thread dump, which is how a concurrency bug is usually diagnosed in production.
 * <p>
 * A thread dump is a snapshot of every thread: its state, the locks it holds, and the lock it is
 * waiting for. In a live process you take one with {@code jcmd <pid> Thread.print} or {@code jstack
 * <pid>}; this class produces the same information through {@link ThreadMXBean} so the format can be
 * shown alongside code that deliberately puts threads into each state.
 *
 * <h2>The states, and what each one means for you</h2>
 * <ul>
 *   <li><b>RUNNABLE</b> is running or wants to be. Note it also covers a thread blocked in a socket
 *       read, because the JVM cannot tell the difference; a dump full of RUNNABLE threads sitting in
 *       network code is not a busy system.</li>
 *   <li><b>BLOCKED</b> is waiting to enter a {@code synchronized} block. The dump names the monitor and
 *       its owner, so a pile of BLOCKED threads naming one monitor is lock contention, and that owner
 *       is what to look at.</li>
 *   <li><b>WAITING</b> and <b>TIMED_WAITING</b> mean the thread parked itself: {@code Object.wait()},
 *       {@code Thread.sleep()}, {@code Lock.lock()}, a queue take, a latch await. Normal for an idle
 *       pool, suspicious when a thread that should be working is parked on a handoff that never comes.</li>
 * </ul>
 *
 * <h2>The distinction that matters most</h2>
 * BLOCKED and WAITING look similar and mean different things. BLOCKED is contention: someone else holds
 * the monitor and this thread will proceed when they release it. WAITING is a handoff: the thread has
 * parked until another thread signals it, and if nobody does, it stays there forever. Contention is a
 * throughput problem, a missing signal is a hang.
 * <p>
 * Also note that {@link java.util.concurrent.locks.ReentrantLock} does not produce BLOCKED. A thread
 * waiting for it is WAITING on an ownable synchronizer, so searching a dump only for BLOCKED misses
 * every lock in {@code java.util.concurrent}.
 */
public final class ThreadDumpExample {

    /** Enough frames to identify a call site. */
    private static final int STACK_FRAMES = 6;

    private ThreadDumpExample() {
    }

    /**
     * Captures the current state of the named threads.
     *
     * @param threads the threads to inspect.
     * @return each thread's name mapped to its state.
     */
    public static Map<String, Thread.State> statesOf(Thread... threads) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = Arrays.stream(threads).mapToLong(Thread::threadId).toArray();
        return Arrays.stream(bean.getThreadInfo(ids))
                .filter(info -> info != null)
                .collect(Collectors.toMap(ThreadInfo::getThreadName, ThreadInfo::getThreadState));
    }

    /**
     * Renders the given threads the way {@code jstack} would.
     *
     * @param threads the threads to dump.
     * @return the rendered dump.
     */
    public static String dump(Thread... threads) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = Arrays.stream(threads).mapToLong(Thread::threadId).toArray();
        ThreadInfo[] infos = bean.getThreadInfo(ids, true, true);

        StringBuilder text = new StringBuilder();
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            text.append('"').append(info.getThreadName()).append("\" id=").append(info.getThreadId())
                    .append(' ').append(info.getThreadState()).append('\n');

            if (info.getLockInfo() != null) {
                text.append("    waiting on ").append(info.getLockInfo());
                if (info.getLockOwnerName() != null) {
                    text.append(" held by \"").append(info.getLockOwnerName()).append('"');
                }
                text.append('\n');
            }
            Arrays.stream(info.getLockedMonitors())
                    .forEach(monitor -> text.append("    holds monitor ").append(monitor).append('\n'));
            Arrays.stream(info.getLockedSynchronizers())
                    .forEach(sync -> text.append("    holds synchronizer ").append(sync).append('\n'));

            StackTraceElement[] stack = info.getStackTrace();
            for (int i = 0; i < Math.min(stack.length, STACK_FRAMES); i++) {
                text.append("        at ").append(stack[i]).append('\n');
            }
            text.append('\n');
        }
        return text.toString();
    }

    /**
     * Puts three threads into three different states and dumps them, so the shapes can be compared.
     * <p>
     * One contends for a monitor someone else holds (BLOCKED), one waits for a notification that never
     * comes (WAITING), and one waits for a ReentrantLock (WAITING too, on an ownable synchronizer,
     * which is the case a BLOCKED-only search would miss).
     *
     * @param args command line arguments (not used).
     * @throws InterruptedException if the demo is interrupted.
     */
    public static void main(String[] args) throws InterruptedException {
        Object monitor = new Object();
        Object neverSignalled = new Object();
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch holdersReady = new CountDownLatch(2);

        Thread monitorHolder = daemon("monitor-holder", () -> {
            synchronized (monitor) {
                holdersReady.countDown();
                sleepForever();
            }
        });
        Thread lockHolder = daemon("lock-holder", () -> {
            lock.lock();
            try {
                holdersReady.countDown();
                sleepForever();
            } finally {
                lock.unlock();
            }
        });
        holdersReady.await();

        Thread blocked = daemon("blocked-on-monitor", () -> {
            synchronized (monitor) {
                throw new IllegalStateException("unreachable while the monitor is held");
            }
        });
        Thread waiting = daemon("waiting-for-notify", () -> {
            synchronized (neverSignalled) {
                try {
                    neverSignalled.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        Thread onLock = daemon("waiting-for-lock", () -> {
            lock.lock();
            lock.unlock();
        });

        // Give the three threads a moment to reach their respective states.
        Thread.sleep(500);

        System.out.println(dump(blocked, waiting, onLock, monitorHolder, lockHolder));
        System.out.println("states: " + statesOf(blocked, waiting, onLock));
    }

    /**
     * Starts a daemon thread so nothing here keeps the JVM alive.
     *
     * @param name the thread name.
     * @param body what it runs.
     * @return the started thread.
     */
    private static Thread daemon(String name, Runnable body) {
        Thread thread = new Thread(body, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** Parks the current thread until the JVM exits. */
    private static void sleepForever() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
