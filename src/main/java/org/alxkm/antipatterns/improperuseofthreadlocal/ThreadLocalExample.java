package org.alxkm.antipatterns.improperuseofthreadlocal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A ThreadLocal value that is set and never removed.
 * <p>
 * The usual warning is that this leaks memory. On a pooled thread something worse happens first: the
 * value outlives the task that set it, and the next task to run on that thread inherits it. A request
 * handler that keeps the current user in a ThreadLocal will hand one user's identity to the next
 * request served by the same thread.
 * <p>
 * {@link #run()} shows it with two requests on a single-thread pool, so both are guaranteed to run on
 * the same thread. Fresh threads would hide the problem, because a thread's values die with it.
 * {@link ThreadLocalCleanupExample} and {@link ThreadLocalWithResourceExample} are the fixes.
 */
public class ThreadLocalExample {
    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Sets a value in the ThreadLocal variable.
     *
     * @param value the value to set.
     */
    public void setThreadLocalValue(String value) {
        THREAD_LOCAL.set(value);
    }

    /**
     * Gets the value from the ThreadLocal variable.
     *
     * @return the value from ThreadLocal.
     */
    public String getThreadLocalValue() {
        return THREAD_LOCAL.get();
    }

    /**
     * Serves two requests on one pooled thread. The first stores its user and never removes it. The
     * second has no user of its own and only reads.
     *
     * @return the user the second request sees: "alice", although it never set one.
     * @throws InterruptedException if this thread is interrupted while waiting for a request.
     * @throws ExecutionException   if a request fails.
     */
    public static String run() throws InterruptedException, ExecutionException {
        ThreadLocalExample example = new ThreadLocalExample();
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            pool.submit(() -> example.setThreadLocalValue("alice")).get();
            return pool.submit(example::getThreadLocalValue).get();
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("second request sees user: " + run()
                + "   <- it never set one; the first request's value was never removed");
    }
}
