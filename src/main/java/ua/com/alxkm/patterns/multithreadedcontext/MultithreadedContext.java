package ua.com.alxkm.patterns.multithreadedcontext;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MultithreadedContext {
    private static volatile MultithreadedContext instance;
    private final Lock lock = new ReentrantLock();

    private MultithreadedContext() {
    }

    public static MultithreadedContext getInstance() {
        if (instance == null) {
            synchronized (MultithreadedContext.class) {
                if (instance == null) {
                    instance = new MultithreadedContext();
                }
            }
        }
        return instance;
    }

    public void apply(Runnable runnable) {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }
}
