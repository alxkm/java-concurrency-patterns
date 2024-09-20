package org.alxkm.antipatterns.improperuseofthreadlocal;

/**
 *
 * Using ThreadLocal incorrectly can lead to memory leaks or unexpected behavior.
 * ThreadLocal variables are meant to provide thread-specific storage that each thread can independently access,
 * but improper usage or not cleaning up properly can cause problems.
 * <p>
 * In this example, the ThreadLocal variable is used to store and retrieve values specific to each thread.
 * However, the values are not removed after usage, which can lead to memory leaks,
 * especially in environments where threads are reused, such as in thread pools.
 *
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

    public static void main(String[] args) {
        ThreadLocalExample example = new ThreadLocalExample();

        Runnable task = () -> {
            example.setThreadLocalValue("ThreadLocal value");
            System.out.println(Thread.currentThread().getName() + ": " + example.getThreadLocalValue());
            // Simulating work
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Not removing the value can cause memory leaks
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
