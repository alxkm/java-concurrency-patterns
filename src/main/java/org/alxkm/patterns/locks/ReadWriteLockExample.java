package org.alxkm.patterns.locks;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ReentrantReadWriteLock: many readers or one writer, and the two rules that decide how to use it.
 * <ul>
 *   <li><b>No upgrade.</b> A thread that holds the read lock cannot take the write lock. The write lock
 *   waits until every reader has left, the thread itself included, so writeLock().lock() blocks forever.
 *   tryLock() shows it without hanging: it returns false even when this thread is the only reader. See
 *   {@link #tryUpgrade()}.</li>
 *   <li><b>Downgrade is fine.</b> A thread that holds the write lock may take the read lock and then
 *   release the write lock. Other readers get in; no writer does, so the value the thread just wrote
 *   cannot change while it still uses it.</li>
 * </ul>
 * {@link CachedValue} puts both together: the cached-data pattern from the ReentrantReadWriteLock
 * javadoc. It is also the place where the usual mistake is made: to load a missing value, a reader has
 * to let go of the read lock before taking the write lock, and in that gap another thread may load it
 * first. So the write path checks again.
 * <p>
 * A read-write lock pays only when reads are many, take a while, and rarely overlap a write. It costs
 * more per acquisition than a plain lock, and with short reads a ReentrantLock or synchronized is
 * usually as fast or faster.
 */
public final class ReadWriteLockExample {

    private ReadWriteLockExample() {
    }

    /**
     * A value loaded on first use and reloaded after {@link #invalidate()}. Any number of threads read
     * it at once; loading and invalidating are exclusive.
     *
     * @param <T> the value's type.
     */
    public static final class CachedValue<T> {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final Supplier<T> loader;
        private T value;
        private boolean valid;

        /**
         * @param loader computes the value. Called under the write lock, so at most one load at a time.
         */
        public CachedValue(Supplier<T> loader) {
            this.loader = loader;
        }

        /**
         * Loads the value if needed and passes it to {@code use}, under the read lock. Nobody can
         * invalidate it while {@code use} runs.
         *
         * @param use what to do with the value.
         * @param <R> what it returns.
         * @return what {@code use} returned.
         */
        public <R> R read(Function<? super T, ? extends R> use) {
            lock.readLock().lock();
            if (!valid) {
                // Must let go of the read lock first: writeLock().lock() while holding it would never return.
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // Check again: in the gap above, another thread may have taken the write lock and loaded.
                    if (!valid) {
                        value = loader.get();
                        valid = true;
                    }
                    // Downgrade: take the read lock before letting go of the write lock, so no writer can
                    // get in between and invalidate what was just loaded.
                    lock.readLock().lock();
                } finally {
                    lock.writeLock().unlock();
                }
            }
            try {
                return use.apply(value);
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Drops the value, so the next read loads it again. Waits for readers that are using it.
         */
        public void invalidate() {
            lock.writeLock().lock();
            try {
                value = null;
                valid = false;
            } finally {
                lock.writeLock().unlock();
            }
        }

        ReentrantReadWriteLock lock() {
            return lock;
        }
    }

    /**
     * Takes the read lock and tries to take the write lock as well.
     *
     * @return whether the write lock was granted; always false. With lock() instead of tryLock() the
     * thread would wait for itself to stop reading, forever.
     */
    public static boolean tryUpgrade() {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.readLock().lock();
        try {
            boolean upgraded = lock.writeLock().tryLock();
            if (upgraded) {
                lock.writeLock().unlock();
            }
            return upgraded;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Takes the write lock, then the read lock, then lets go of the write lock.
     *
     * @return whether the read lock was granted while holding the write lock; always true.
     */
    public static boolean tryDowngrade() {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            boolean downgraded = lock.readLock().tryLock();
            if (downgraded) {
                lock.readLock().unlock();
            }
            return downgraded;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void main(String[] args) {
        System.out.println("read lock held, writeLock().tryLock(): " + tryUpgrade()
                + "  <- no upgrade; lock() would wait forever");
        System.out.println("write lock held, readLock().tryLock(): " + tryDowngrade()
                + "  <- downgrade is allowed");

        int[] loads = {0};
        CachedValue<String> config = new CachedValue<>(() -> "config v" + ++loads[0]);
        System.out.println(config.read(v -> v) + ", " + config.read(v -> v) + " (loaded once)");
        config.invalidate();
        System.out.println(config.read(v -> v) + " (loaded again after invalidate)");
    }
}
