package org.alxkm.patterns.multithreadedcontext;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The MultithreadedContext class provides a thread-safe context for executing operations.
 */
public final class MultithreadedContext {
    private static final MultithreadedContext INSTANCE = new MultithreadedContext();
    private final Lock lock = new ReentrantLock();

    /**
     * Private constructor to prevent instantiation.
     */
    private MultithreadedContext() {}

    /**
     * Returns the singleton instance of MultithreadedContext.
     *
     * @return the singleton instance
     */
    public static MultithreadedContext getInstance() {
        return INSTANCE;
    }

    /**
     * Executes a Runnable operation in a thread-safe context.
     *
     * @param runnable the operation to be executed
     */
    public void apply(Runnable runnable) {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Attempts to execute a Runnable operation in a thread-safe context without blocking.
     * If the lock is not available, the operation will not be executed.
     *
     * @param runnable the operation to be executed
     * @return true if the operation was executed successfully, false otherwise
     */
    public boolean tryApply(Runnable runnable) {
        if (lock.tryLock()) {
            try {
                runnable.run();
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * Main method demonstrating the usage of MultithreadedContext.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        MultithreadedContext context = MultithreadedContext.getInstance();

        Runnable operation = () -> {
            System.out.println("Thread-safe operation is performed.");
        };

        context.apply(operation);
    }
}
