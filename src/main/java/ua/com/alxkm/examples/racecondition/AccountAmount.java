package ua.com.alxkm.examples.racecondition;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AccountAmount {
    private int amount;
    private final AtomicInteger atomicInteger = new AtomicInteger(0);
    private final Lock lock = new ReentrantLock();

    public int getAmount() {
        return amount;
    }

    public void unsafeIncrementAmount() {
        amount++;
    }

    public void safeLockIncrementAmount() {
        while (true) {
            if (lock.tryLock()) {
                try {
                    amount++;
                    break;
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public void safeSynchronizedIncrementAmount() {
        synchronized (AccountAmount.class) {
            amount++;
        }
    }

    public void incrementAtomicAmount() {
        atomicInteger.incrementAndGet();
    }

    public AtomicInteger getAtomicInteger() {
        return atomicInteger;
    }
}
