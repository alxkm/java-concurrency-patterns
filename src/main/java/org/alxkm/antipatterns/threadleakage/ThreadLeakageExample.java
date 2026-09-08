package org.alxkm.antipatterns.threadleakage;

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

    public static void main(String[] args) {
        ThreadLeakageExample example = new ThreadLeakageExample();
        example.startThreads(TASK_COUNT);

        System.out.println("Live threads after spawning " + TASK_COUNT + " of them: "
                + Thread.currentThread().getThreadGroup().activeCount());
    }

    /**
     * Starts one brand new thread per task and never joins or reuses any of them, which is what leaks.
     *
     * @param taskCount how many tasks -- and therefore how many threads -- to start
     */
    public void startThreads(int taskCount) {
        for (int i = 0; i < taskCount; i++) {
            new Thread(() -> {
                // Simulate some work with a sleep
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
