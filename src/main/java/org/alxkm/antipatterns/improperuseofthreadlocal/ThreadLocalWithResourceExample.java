package org.alxkm.antipatterns.improperuseofthreadlocal;

/**
 * Java 7 introduced the AutoCloseable interface and try-with-resources statement,
 * which can be adapted for ThreadLocal cleanup using a custom utility class.
 * <p>
 * In this version, the ThreadLocalCleaner class implements AutoCloseable, allowing it to be used in a try-with-resources statement to ensure ThreadLocal cleanup.
 */
public class ThreadLocalWithResourceExample {
    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Custom class to handle ThreadLocal cleanup using AutoCloseable.
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

    // The cleaner is never referenced in the try body on purpose: it is held solely so that its
    // close() runs the ThreadLocal.remove() on the way out, which is what -Xlint:try warns about.
    @SuppressWarnings("try")
    public static void main(String[] args) throws InterruptedException {
        ThreadLocalWithResourceExample example = new ThreadLocalWithResourceExample();

        Runnable task = () -> {
            try (ThreadLocalCleaner cleaner = new ThreadLocalCleaner()) {
                example.setThreadLocalValue("ThreadLocal value");
                System.out.println(Thread.currentThread().getName() + ": " + example.getThreadLocalValue());
                // Simulating work
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
    }
}

