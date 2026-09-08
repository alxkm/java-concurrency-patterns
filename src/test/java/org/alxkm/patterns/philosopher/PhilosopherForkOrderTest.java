package org.alxkm.patterns.philosopher;

import org.alxkm.testsupport.Await;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the property that makes the dining philosophers solution deadlock-free, rather than
 * watching for the deadlock itself.
 * <p>
 * Waiting to observe a deadlock makes a poor test. In the naive left-then-right version the two
 * {@code lock()} calls are back to back, so the window in which a philosopher holds exactly one fork
 * is nanoseconds wide and the cycle almost never closes -- which is precisely what makes that bug
 * dangerous rather than harmless. A test that waits for a hang therefore passes against the broken
 * code nearly every time.
 * <p>
 * The invariant behind the fix is deterministic and can be asserted directly: every philosopher must
 * take the lower-numbered fork first. If that holds for all of them, no cycle can form in the
 * wait-for graph, and deadlock is impossible by construction.
 */
class PhilosopherForkOrderTest {
    private static final int PHILOSOPHERS = 5;

    @Test
    void everyPhilosopherTakesTheLowerNumberedForkFirst() throws InterruptedException {
        ConcurrentLinkedQueue<Acquisition> log = new ConcurrentLinkedQueue<>();
        Lock[] forks = new Lock[PHILOSOPHERS];
        for (int i = 0; i < PHILOSOPHERS; i++) {
            forks[i] = new RecordingLock(i, log);
        }

        PhilosopherWithLock[] philosophers = new PhilosopherWithLock[PHILOSOPHERS];
        for (int i = 0; i < PHILOSOPHERS; i++) {
            // Philosopher 4 sits between fork 4 and fork 0, so it is the one whose "left then right"
            // order would run counter to the global ordering. It is the interesting case.
            philosophers[i] = new PhilosopherWithLock(i, i, (i + 1) % PHILOSOPHERS, forks);
        }
        for (PhilosopherWithLock philosopher : philosophers) {
            philosopher.start();
        }

        try {
            Await.until("each philosopher to pick up forks a few times",
                    () -> log.stream().collect(Collectors.groupingBy(a -> a.thread, Collectors.counting()))
                            .values().stream().filter(c -> c >= 4).count() == PHILOSOPHERS);
        } finally {
            for (PhilosopherWithLock philosopher : philosophers) {
                philosopher.interrupt();
            }
            for (PhilosopherWithLock philosopher : philosophers) {
                philosopher.join(TimeUnit.SECONDS.toMillis(5));
                assertFalse(philosopher.isAlive(), "philosopher did not stop when interrupted");
            }
        }

        Map<String, List<Integer>> perThread = log.stream().collect(
                Collectors.groupingBy(a -> a.thread, Collectors.mapping(a -> a.fork, Collectors.toList())));

        for (Map.Entry<String, List<Integer>> entry : perThread.entrySet()) {
            List<Integer> acquisitions = entry.getValue();
            // Each meal contributes exactly two acquisitions, in the order they were taken.
            for (int i = 0; i + 1 < acquisitions.size(); i += 2) {
                int first = acquisitions.get(i);
                int second = acquisitions.get(i + 1);
                assertTrue(first < second,
                        entry.getKey() + " took fork " + first + " before fork " + second
                                + ", which breaks the global fork ordering and admits deadlock");
            }
        }
    }

    private record Acquisition(String thread, int fork) {
    }

    /**
     * A {@link Lock} that records the order in which threads acquire it, delegating everything else
     * to a plain {@link ReentrantLock}.
     */
    private static final class RecordingLock implements Lock {
        private final int id;
        private final ConcurrentLinkedQueue<Acquisition> log;
        private final ReentrantLock delegate = new ReentrantLock();

        RecordingLock(int id, ConcurrentLinkedQueue<Acquisition> log) {
            this.id = id;
            this.log = log;
        }

        @Override
        public void lock() {
            delegate.lock();
            log.add(new Acquisition(Thread.currentThread().getName(), id));
        }

        @Override
        public void unlock() {
            delegate.unlock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            delegate.lockInterruptibly();
            log.add(new Acquisition(Thread.currentThread().getName(), id));
        }

        @Override
        public boolean tryLock() {
            boolean acquired = delegate.tryLock();
            if (acquired) {
                log.add(new Acquisition(Thread.currentThread().getName(), id));
            }
            return acquired;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            boolean acquired = delegate.tryLock(time, unit);
            if (acquired) {
                log.add(new Acquisition(Thread.currentThread().getName(), id));
            }
            return acquired;
        }

        @Override
        public Condition newCondition() {
            return delegate.newCondition();
        }
    }
}
