package org.alxkm.patterns.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A mutex built on AbstractQueuedSynchronizer, the framework behind ReentrantLock, Semaphore,
 * CountDownLatch and FutureTask.
 * <p>
 * AQS owns the hard part: the queue of waiting threads, parking and waking them, timeouts and
 * interrupts. A synchronizer only says what its int state means and when it may be taken. Here
 * 0 is unlocked and 1 is locked, and the whole lock is the three hooks in {@link Mutex.Sync}.
 * <p>
 * The hook people get wrong is tryRelease. Setting the state back to 0 is not enough: without the
 * owner check, any thread can call unlock() and let a second thread in while the first is still
 * inside its critical section. The owner is recorded with setExclusiveOwnerThread, which AQS
 * inherits from AbstractOwnableSynchronizer for exactly this.
 */
public final class AbstractQueuedSynchronizerExample {

    private AbstractQueuedSynchronizerExample() {
    }

    /**
     * A non-reentrant lock: a thread that already holds it and calls lock() again waits for itself
     * forever, and tryLock() returns false. ReentrantLock counts the holds in the state instead.
     */
    public static final class Mutex {
        private final Sync sync = new Sync();

        private static final class Sync extends AbstractQueuedSynchronizer {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean tryAcquire(int ignored) {
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }

            @Override
            protected boolean tryRelease(int ignored) {
                if (getExclusiveOwnerThread() != Thread.currentThread()) {
                    throw new IllegalMonitorStateException("the mutex is not held by " + Thread.currentThread());
                }
                setExclusiveOwnerThread(null);
                // The state is volatile, so this write publishes everything done while the lock was held
                // to the next thread that reads the state in tryAcquire.
                setState(0);
                return true;
            }

            @Override
            protected boolean isHeldExclusively() {
                return getExclusiveOwnerThread() == Thread.currentThread();
            }

            boolean isLocked() {
                return getState() == 1;
            }
        }

        /**
         * Takes the lock, waiting in the AQS queue as long as it takes.
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * Takes the lock only if it is free right now. Does not queue.
         *
         * @return true if the lock was taken.
         */
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        /**
         * Takes the lock, waiting at most the given time.
         *
         * @param timeout how long to wait.
         * @param unit    the unit of the timeout.
         * @return true if the lock was taken, false if the time ran out.
         * @throws InterruptedException if the thread is interrupted while waiting.
         */
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * Releases the lock and wakes the first waiting thread, if any.
         *
         * @throws IllegalMonitorStateException if the calling thread does not hold the lock.
         */
        public void unlock() {
            sync.release(1);
        }

        /**
         * @return true if some thread holds the lock.
         */
        public boolean isLocked() {
            return sync.isLocked();
        }

        /**
         * @return true if the calling thread holds the lock.
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * @return how many threads are queued waiting for the lock. An estimate while threads come and go.
         */
        public int getQueueLength() {
            return sync.getQueueLength();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Mutex mutex = new Mutex();
        int[] counter = {0};

        Runnable increment = () -> {
            for (int i = 0; i < 100_000; i++) {
                mutex.lock();
                try {
                    counter[0]++;
                } finally {
                    mutex.unlock();
                }
            }
        };
        Thread first = new Thread(increment);
        Thread second = new Thread(increment);
        first.start();
        second.start();
        first.join();
        second.join();
        System.out.println("Counter after 2 x 100000 increments under the mutex: " + counter[0]);

        mutex.lock();
        Thread intruder = new Thread(() -> {
            try {
                mutex.unlock();
                System.out.println("Another thread released a lock it did not hold");
            } catch (IllegalMonitorStateException e) {
                System.out.println("Another thread tried to unlock: " + e.getMessage());
            }
        });
        intruder.start();
        intruder.join();
        System.out.println("Still locked by main: " + mutex.isHeldByCurrentThread());
        mutex.unlock();
    }
}
