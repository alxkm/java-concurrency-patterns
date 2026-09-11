package org.alxkm.antipatterns.threadleakage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demonstrates thread leakage by creating and starting new threads for every unit of work, without any pool
 * to bound them or reuse them.
 * <p>
 * Each thread costs a stack and an OS-level thread, and none of them is reused, so the cost grows with the
 * amount of work rather than staying flat. The demo spawns a bounded number of threads instead of looping
 * forever: the point is to show the unbounded growth in thread count, not to actually exhaust the machine.
 * Compare with {@link ThreadLeakageResolution}, which does the same work on a fixed pool.
 */
public class ThreadLeakageExample {

    private static final int TASK_COUNT = 100;

    /** Short enough to keep the demo quick, long enough that the threads genuinely overlap. */
    private static final long WORK_MILLIS = 50;

    public static void main(String[] args) throws InterruptedException {
        ThreadLeakageExample example = new ThreadLeakageExample();
        int threadsUsed = example.startThreads(TASK_COUNT);

        System.out.println("Ran " + TASK_COUNT + " tasks on " + threadsUsed + " distinct threads.");
    }

    /**
     * Starts one brand new thread per task and never joins or reuses any of them, which is what leaks.
     *
     * @param taskCount how many tasks, and therefore how many threads, to start
     * @return the number of distinct threads that ran the tasks, which equals {@code taskCount}
     * @throws InterruptedException if this thread is interrupted while joining
     */
    public int startThreads(int taskCount) throws InterruptedException {
        Set<Long> threadIds = ConcurrentHashMap.newKeySet();
        List<Thread> started = new ArrayList<>(taskCount);

        for (int i = 0; i < taskCount; i++) {
            Thread thread = new Thread(() -> {
                threadIds.add(Thread.currentThread().threadId());
                // Simulate some work with a sleep
                try {
                    Thread.sleep(WORK_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            started.add(thread);
            thread.start();
        }

        for (Thread thread : started) {
            thread.join();
        }
        return threadIds.size();
    }
}
