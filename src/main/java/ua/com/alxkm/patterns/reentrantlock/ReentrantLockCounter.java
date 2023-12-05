package ua.com.alxkm.patterns.reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockCounter {
    private final ReentrantLock lock = new ReentrantLock();
    private int counter;

    public void incrementCounter() {
        lock.lock();
        try {
            counter++;
        } finally {
            lock.unlock();
        }
    }

    public int getCounter() {
        return counter;
    }
}
