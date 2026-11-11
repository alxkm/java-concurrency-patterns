package org.alxkm.patterns.locks;

import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

/**
 * A byte budget built on AbstractQueuedLongSynchronizer: the same framework as
 * AbstractQueuedSynchronizer, with a long state instead of an int.
 * <p>
 * The long is the reason to pick it. A Semaphore counts permits in an int, so it cannot hold a
 * budget of more than about 2 GB if one permit is one byte. Here the state is the number of bytes
 * still free, and the budget can be as large as a long.
 * <p>
 * It also shows the other half of AQS, shared mode. A mutex lets one thread in at a time; here any
 * number of threads can hold part of the budget at once, as long as the parts fit. That is
 * tryAcquireShared and tryReleaseShared instead of tryAcquire and tryRelease.
 */
public final class AbstractQueuedLongSynchronizerExample {

    private AbstractQueuedLongSynchronizerExample() {
    }

    /**
     * Limits how many bytes are in use at once, for example how much data is buffered in memory.
     * A caller takes the bytes it needs before the work and gives them back after.
     */
    public static final class ByteBudget {
        private final long capacity;
        private final Sync sync;

        /**
         * @param capacity the total number of bytes; may be more than Integer.MAX_VALUE.
         */
        public ByteBudget(long capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive: " + capacity);
            }
            this.capacity = capacity;
            this.sync = new Sync(capacity);
        }

        private static final class Sync extends AbstractQueuedLongSynchronizer {
            private static final long serialVersionUID = 1L;

            Sync(long capacity) {
                setState(capacity);
            }

            /**
             * Negative means it did not fit and the thread waits. Zero or more means it was taken. The
             * contract gives zero and positive different meanings: zero says nothing is left for the
             * next waiter, positive says the next one might fit and must be allowed to check. The JDK
             * 21 implementation wakes the next shared waiter either way, but the contract is what a
             * synchronizer should rely on.
             */
            @Override
            protected long tryAcquireShared(long bytes) {
                while (true) {
                    long available = getState();
                    long remaining = available - bytes;
                    // Read, compute and CAS in a loop: another thread may change the state in between.
                    if (remaining < 0 || compareAndSetState(available, remaining)) {
                        return remaining;
                    }
                }
            }

            @Override
            protected boolean tryReleaseShared(long bytes) {
                while (true) {
                    long available = getState();
                    if (compareAndSetState(available, available + bytes)) {
                        // True tells AQS to wake the waiters, so they can check whether they fit now.
                        return true;
                    }
                }
            }

            long available() {
                return getState();
            }
        }

        /**
         * Takes the bytes, waiting until that many are free.
         *
         * @param bytes how many bytes to take; from 1 to the capacity.
         * @throws InterruptedException if the thread is interrupted while waiting.
         */
        public void acquire(long bytes) throws InterruptedException {
            sync.acquireSharedInterruptibly(checked(bytes));
        }

        /**
         * Takes the bytes only if that many are free right now. Does not queue.
         *
         * @param bytes how many bytes to take; from 1 to the capacity.
         * @return true if the bytes were taken.
         */
        public boolean tryAcquire(long bytes) {
            return sync.tryAcquireShared(checked(bytes)) >= 0;
        }

        /**
         * Gives bytes back and wakes the threads waiting for them.
         *
         * @param bytes how many bytes to give back; the caller must have taken them.
         */
        public void release(long bytes) {
            sync.releaseShared(checked(bytes));
        }

        /**
         * @return how many bytes are free right now.
         */
        public long available() {
            return sync.available();
        }

        /**
         * @return how many threads are queued waiting for bytes. An estimate while threads come and go.
         */
        public int getQueueLength() {
            return sync.getQueueLength();
        }

        // A request larger than the capacity could never be met, and the thread would wait forever.
        private long checked(long bytes) {
            if (bytes <= 0 || bytes > capacity) {
                throw new IllegalArgumentException("bytes must be from 1 to " + capacity + ": " + bytes);
            }
            return bytes;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long gib = 1L << 30;
        ByteBudget budget = new ByteBudget(8 * gib);
        System.out.println("Budget: " + budget.available() / gib + " GiB, more than an int can count");

        budget.acquire(6 * gib);
        System.out.println("Loader took 6 GiB, free: " + budget.available() / gib + " GiB");

        Thread second = new Thread(() -> {
            try {
                System.out.println("Second loader asks for 4 GiB and waits");
                budget.acquire(4 * gib);
                System.out.println("Second loader got 4 GiB, free: " + budget.available() / gib + " GiB");
                budget.release(4 * gib);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        second.start();
        while (budget.getQueueLength() == 0) {
            Thread.onSpinWait();
        }

        System.out.println("Loader gives back its 6 GiB");
        budget.release(6 * gib);
        second.join();
        System.out.println("Free at the end: " + budget.available() / gib + " GiB");
    }
}
