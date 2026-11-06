package org.alxkm.antipatterns.improperuseofthreadlocal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The fix for {@link ThreadLocalExample}: remove the value in a finally block, so it is gone when the
 * task ends, however it ends.
 * <p>
 * {@link #run()} serves the same two requests on one pooled thread, and the second one now sees no
 * user.
 */
public class ThreadLocalCleanupExample {
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
     * Removes the value from the ThreadLocal variable, so the next task on this thread starts clean.
     */
    public void removeThreadLocalValue() {
        THREAD_LOCAL.remove();
    }

    /**
     * Serves two requests on one pooled thread. The first stores its user and removes it in a finally
     * block. The second has no user of its own and only reads.
     *
     * @return the user the second request sees: null.
     * @throws InterruptedException if this thread is interrupted while waiting for a request.
     * @throws ExecutionException   if a request fails.
     */
    public static String run() throws InterruptedException, ExecutionException {
        ThreadLocalCleanupExample example = new ThreadLocalCleanupExample();
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            pool.submit(() -> {
                try {
                    example.setThreadLocalValue("alice");
                } finally {
                    example.removeThreadLocalValue();
                }
            }).get();
            return pool.submit(example::getThreadLocalValue).get();
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("second request sees user: " + run()
                + "   <- the first request removed its value in finally");
    }
}
