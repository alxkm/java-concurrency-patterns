package org.alxkm.patterns.locks;

import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates the usage of AbstractOwnableSynchronizer in a multithreaded environment.
 * <p>
 * AbstractOwnableSynchronizer provides a basic framework for synchronizers that may be exclusively owned by threads.
 */
public class AbstractOwnableSynchronizerExample {

    /**
     * Custom synchronizer that extends AbstractOwnableSynchronizer.
     */
    static class CustomSynchronizer extends AbstractOwnableSynchronizer {

        private final Lock lock = new ReentrantLock(); // Internal lock

        /**
         * Acquires the lock.
         */
        void lock() {
            lock.lock();
            // Set the owner of the synchronizer to the current thread
            setExclusiveOwnerThread(Thread.currentThread());
        }

        /**
         * Releases the lock.
         */
        void unlock() {
            // Clear the owner of the synchronizer
            setExclusiveOwnerThread(null);
            lock.unlock();
        }

        /**
         * Indicates whether the current thread is the owner of the lock.
         *
         * @return true if the current thread is the owner, otherwise false.
         */
        boolean isOwner() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    /**
     * We define a custom synchronizer CustomSynchronizer that extends AbstractOwnableSynchronizer.
     * The lock() method acquires the lock and sets the current thread as the owner.
     * The unlock() method releases the lock and clears the owner.
     * The isOwner() method checks whether the current thread is the owner of the lock.
     * We create an instance of CustomSynchronizer.
     * We define two worker threads: lockThread and tryLockThread.
     * lockThread acquires the lock using lock() method and releases it using unlock() method.
     * tryLockThread tries to acquire the lock using tryLock() method and releases it if acquired, or prints a failure message if not.
     * Both threads run concurrently.
     * After both threads have completed, the main thread prints the final status.
     */

    public static void main(String[] args) {
        // Create an instance of CustomSynchronizer
        CustomSynchronizer synchronizer = new CustomSynchronizer();

        // Define worker threads
        Thread lockThread = new Thread(() -> {
            synchronizer.lock(); // Acquire the lock
            System.out.println(Thread.currentThread().getName() + " acquired the lock.");
            synchronizer.unlock(); // Release the lock
            System.out.println(Thread.currentThread().getName() + " released the lock.");
        });

        Thread tryLockThread = new Thread(() -> {
            boolean acquired = synchronizer.lock.tryLock(); // Try to acquire the lock
            if (acquired) {
                System.out.println(Thread.currentThread().getName() + " acquired the lock.");
                synchronizer.unlock(); // Release the lock if acquired
                System.out.println(Thread.currentThread().getName() + " released the lock.");
            } else {
                System.out.println(Thread.currentThread().getName() + " failed to acquire the lock.");
            }
        });

        // Start worker threads
        lockThread.start();
        tryLockThread.start();

        // Wait for worker threads to complete
        try {
            lockThread.join();
            tryLockThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
