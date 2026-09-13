package org.alxkm.antipatterns.threadsinsteadoftasks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Directly creating and managing threads instead of using the Executor framework can lead to poor scalability and complex error handling.
 * <p>
 * In this example, tasks are executed by directly creating and managing threads.
 * This approach can lead to problems such as unbounded thread creation, poor resource management, and difficulty in handling errors.
 */
public class DirectThreadManagement {
    /** Task count used by the demo in main. */
    private static final int DEMO_TASKS = 2;

    /**
     * Runs two tasks, each on a thread of its own.
     *
     * @return how many distinct threads ran the tasks.
     * @throws InterruptedException if the current thread is interrupted while joining the workers.
     */
    public int performTask() throws InterruptedException {
        return performTask(DEMO_TASKS);
    }

    /**
     * Runs the given number of tasks, creating one thread per task.
     *
     * The thread count is the whole point: it tracks the number of tasks, so twice the work means
     * twice the threads and there is no upper bound on either. Compare with
     * {@link ExecutorFrameworkExample}, where the same work runs on a fixed pool.
     *
     * @param taskCount how many tasks to run.
     * @return how many distinct threads ran the tasks, which equals taskCount.
     * @throws InterruptedException if the current thread is interrupted while joining the workers.
     */
    public int performTask(int taskCount) throws InterruptedException {
        Set<Long> threadIds = ConcurrentHashMap.newKeySet();
        List<Thread> threads = new ArrayList<>(taskCount);

        Runnable task = () -> {
            threadIds.add(Thread.currentThread().threadId());
            System.out.println("Task executed by: " + Thread.currentThread().getName());
        };

        // Directly creating and starting threads
        for (int i = 0; i < taskCount; i++) {
            Thread thread = new Thread(task);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return threadIds.size();
    }

    public static void main(String[] args) throws InterruptedException {
        DirectThreadManagement manager = new DirectThreadManagement();
        System.out.println("Threads used: " + manager.performTask());
    }
}
