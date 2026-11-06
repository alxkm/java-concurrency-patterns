package org.alxkm.antipatterns.improperuseofthreadlocal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The same fix as {@link ThreadLocalCleanupExample}, written with try-with-resources. A cleaner whose
 * close() removes the value is harder to forget than a finally block someone has to remember to write.
 * <p>
 * {@link #run()} serves the same two requests on one pooled thread, and the second one sees no user.
 */
public class ThreadLocalWithResourceExample {
    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Removes the ThreadLocal value when closed, so it can be used in a try-with-resources statement.
     */
    public static class ThreadLocalCleaner implements AutoCloseable {
        @Override
        public void close() {
            THREAD_LOCAL.remove();
        }
    }

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
     * Serves two requests on one pooled thread. The first stores its user inside a try-with-resources
     * block whose cleaner removes it. The second has no user of its own and only reads.
     *
     * @return the user the second request sees: null.
     * @throws InterruptedException if this thread is interrupted while waiting for a request.
     * @throws ExecutionException   if a request fails.
     */
    // The cleaner is never referenced in the try body on purpose: it is held solely so that its
    // close() runs the ThreadLocal.remove() on the way out, which is what -Xlint:try warns about.
    @SuppressWarnings({"try", "PMD.UnusedLocalVariable"})
    public static String run() throws InterruptedException, ExecutionException {
        ThreadLocalWithResourceExample example = new ThreadLocalWithResourceExample();
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            pool.submit(() -> {
                try (ThreadLocalCleaner cleaner = new ThreadLocalCleaner()) {
                    example.setThreadLocalValue("alice");
                }
            }).get();
            return pool.submit(example::getThreadLocalValue).get();
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("second request sees user: " + run()
                + "   <- the cleaner's close() removed the first request's value");
    }
}
