package org.alxkm.antipatterns.improperuseofthreadlocal;

/**
 * To avoid memory leaks, ensure that ThreadLocal variables are cleaned up after use by calling the remove method.
 * <p>
 * <p>
 * In this revised example, the removeThreadLocalValue method is called in the finally block to ensure that the ThreadLocal variable is cleaned up after use, preventing memory leaks.
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
     * Removes the value from the ThreadLocal variable to prevent memory leaks.
     */
    public void removeThreadLocalValue() {
        THREAD_LOCAL.remove();
    }

    public static void main(String[] args) {
        ThreadLocalCleanupExample example = new ThreadLocalCleanupExample();

        Runnable task = () -> {
            try {
                example.setThreadLocalValue("ThreadLocal value");
                System.out.println(Thread.currentThread().getName() + ": " + example.getThreadLocalValue());
                // Simulating work
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // Ensure the ThreadLocal variable is cleaned up
                example.removeThreadLocalValue();
            }
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

