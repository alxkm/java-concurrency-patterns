package org.alxkm.diagnostics;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds deadlocks in a running JVM and reports them in a form you can act on.
 * <p>
 * The JVM can detect a deadlock itself: {@link ThreadMXBean#findDeadlockedThreads()} walks the lock
 * graph and returns any cycle of threads each waiting on a lock another one holds. It covers both
 * intrinsic monitors and ownable synchronizers such as {@link java.util.concurrent.locks.ReentrantLock}.
 * This is the same analysis that puts a "Found one Java-level deadlock" section at the end of a
 * {@code jcmd &lt;pid&gt; Thread.print} dump.
 * <p>
 * Two things are worth knowing before relying on it.
 * <p>
 * First, it is JVM-wide. Any deadlock anywhere in the process shows up, including ones some unrelated
 * component created on purpose. Code that asserts on the result needs {@link #deadlockedAmong(Thread...)}
 * to narrow it to threads it actually started, or it will fail because of somebody else's cycle.
 * <p>
 * Second, it only finds cycles. A thread blocked forever on a lock nobody will ever release is not a
 * deadlock by this definition and will not be reported, and neither will a livelock, where threads keep
 * running but make no progress. For those you need the full thread dump and your own eyes.
 */
public final class DeadlockDetector {

    private DeadlockDetector() {
    }

    /**
     * Returns the ids of all deadlocked threads in this JVM, or null if there are none.
     *
     * @return the deadlocked thread ids, or null.
     */
    public static long[] deadlockedThreads() {
        return ManagementFactory.getThreadMXBean().findDeadlockedThreads();
    }

    /**
     * Returns the deadlocked subset of the given threads, or null if none of them are deadlocked.
     * <p>
     * Use this rather than {@link #deadlockedThreads()} whenever the answer feeds an assertion. The
     * JVM-wide view includes cycles other code created, which makes a test fail for reasons that have
     * nothing to do with it.
     *
     * @param threads the threads to consider.
     * @return the deadlocked subset of their ids, or null if none of them are deadlocked.
     */
    public static long[] deadlockedAmong(Thread... threads) {
        long[] deadlocked = deadlockedThreads();
        if (deadlocked == null) {
            return null;
        }
        Set<Long> ours = Arrays.stream(threads).map(Thread::threadId).collect(Collectors.toSet());
        long[] mine = Arrays.stream(deadlocked).filter(ours::contains).toArray();
        return mine.length == 0 ? null : mine;
    }

    /**
     * Describes every deadlock in this JVM the way a thread dump would.
     * <p>
     * For each thread in the cycle it reports the lock it is waiting for, who owns that lock, and the
     * top of its stack, which is normally enough to find the two call sites taking locks in opposite
     * orders.
     *
     * @return a human readable report, or a single line saying no deadlock was found.
     */
    public static String describeDeadlocks() {
        long[] deadlocked = deadlockedThreads();
        if (deadlocked == null) {
            return "No deadlock found.";
        }

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = bean.getThreadInfo(deadlocked, true, true);

        StringBuilder report = new StringBuilder();
        report.append("Found a Java-level deadlock involving ").append(infos.length).append(" threads:\n");
        for (ThreadInfo info : infos) {
            report.append('\n').append(describe(info));
        }
        return report.toString();
    }

    /**
     * Describes one deadlocked thread: what it wants, who has it, and where it is.
     *
     * @param info the thread to describe.
     * @return the description, ending in a newline.
     */
    private static String describe(ThreadInfo info) {
        StringBuilder text = new StringBuilder();
        text.append("\"").append(info.getThreadName()).append("\" id=").append(info.getThreadId())
                .append(' ').append(info.getThreadState()).append('\n');

        LockInfo waitingFor = info.getLockInfo();
        if (waitingFor != null) {
            text.append("    waiting to lock ").append(waitingFor);
            if (info.getLockOwnerName() != null) {
                text.append(" which is held by \"").append(info.getLockOwnerName())
                        .append("\" id=").append(info.getLockOwnerId());
            }
            text.append('\n');
        }

        for (MonitorInfo held : info.getLockedMonitors()) {
            text.append("    holds ").append(held).append('\n');
        }
        for (LockInfo held : info.getLockedSynchronizers()) {
            text.append("    holds ").append(held).append('\n');
        }

        StackTraceElement[] stack = info.getStackTrace();
        int frames = Math.min(stack.length, STACK_FRAMES);
        for (int i = 0; i < frames; i++) {
            text.append("        at ").append(stack[i]).append('\n');
        }
        return text.toString();
    }

    /** Enough frames to identify the call site without reprinting the whole stack. */
    private static final int STACK_FRAMES = 5;

    /**
     * Deadlocks two threads on purpose and prints the report.
     *
     * @param args command line arguments (not used).
     * @throws InterruptedException if the demo is interrupted while waiting.
     */
    public static void main(String[] args) throws InterruptedException {
        Object first = new Object();
        Object second = new Object();
        java.util.concurrent.CountDownLatch bothHoldOne = new java.util.concurrent.CountDownLatch(2);

        Thread a = startHolder("holder-a", first, second, bothHoldOne);
        Thread b = startHolder("holder-b", second, first, bothHoldOne);

        // Wait for the JVM to register the cycle, then report it.
        for (int i = 0; i < 100 && deadlockedAmong(a, b) == null; i++) {
            Thread.sleep(50);
        }
        System.out.println(describeDeadlocks());
    }

    /**
     * Starts a daemon thread that takes {@code hold}, waits for its partner, then blocks on {@code want}.
     *
     * @param name         the thread name.
     * @param hold         the monitor this thread takes first.
     * @param want         the monitor it then blocks on.
     * @param bothHoldOne  latch both threads count down once they hold their first monitor.
     * @return the started thread.
     */
    private static Thread startHolder(String name, Object hold, Object want,
                                      java.util.concurrent.CountDownLatch bothHoldOne) {
        Thread thread = new Thread(() -> {
            synchronized (hold) {
                bothHoldOne.countDown();
                try {
                    // Both threads must hold their first monitor before either reaches for the second,
                    // otherwise one simply takes both and no cycle forms.
                    bothHoldOne.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                synchronized (want) {
                    throw new IllegalStateException("unreachable in a deadlock");
                }
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
