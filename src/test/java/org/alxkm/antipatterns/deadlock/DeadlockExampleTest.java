package org.alxkm.antipatterns.deadlock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DeadlockExampleTest {

    /**
     * Demonstrates that the DeadlockExample actually causes a deadlock
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testDeadlockOccurs() throws InterruptedException {
        DeadlockExample example = new DeadlockExample();
        AtomicBoolean deadlockDetected = new AtomicBoolean(false);
        CountDownLatch testStarted = new CountDownLatch(1);
        
        // Start a thread to monitor for deadlocks
        Thread deadlockMonitor = new Thread(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            testStarted.countDown();
            
            // Check for deadlocks multiple times
            for (int i = 0; i < 50 && !deadlockDetected.get(); i++) {
                long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
                if (deadlockedThreadIds != null && deadlockedThreadIds.length > 0) {
                    deadlockDetected.set(true);
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        deadlockMonitor.start();
        testStarted.await();
        
        // Cause the deadlock
        example.causeDeadlock();
        
        // Wait for deadlock detection
        deadlockMonitor.join();
        
        assertTrue(deadlockDetected.get(), "Deadlock should have been detected");
    }

    /**
     * Test that demonstrates the specific threads involved in the deadlock
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testDeadlockThreadDetails() throws InterruptedException {
        // Create our own locks to simulate the deadlock pattern
        final Object lock1 = new Object();
        final Object lock2 = new Object();
        CountDownLatch bothThreadsStarted = new CountDownLatch(2);
        AtomicBoolean thread1InSync = new AtomicBoolean(false);
        AtomicBoolean thread2InSync = new AtomicBoolean(false);
        
        // Create threads that will deadlock
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                thread1InSync.set(true);
                bothThreadsStarted.countDown();
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                synchronized (lock2) {
                    // This should never be reached in a deadlock
                    fail("Thread 1 should not acquire lock2 in a deadlock");
                }
            }
        }, "DeadlockThread-1");
        
        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                thread2InSync.set(true);
                bothThreadsStarted.countDown();
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                synchronized (lock1) {
                    // This should never be reached in a deadlock
                    fail("Thread 2 should not acquire lock1 in a deadlock");
                }
            }
        }, "DeadlockThread-2");
        
        t1.start();
        t2.start();
        
        // Wait for both threads to start and acquire their first locks
        bothThreadsStarted.await();
        // Both threads hold their first lock, but the JVM has not necessarily registered the cycle
        // yet. Poll for it rather than guessing how long that takes.
        awaitDeadlockAmong(2, t1, t2);
        
        // Verify both threads are stuck
        assertTrue(t1.isAlive());
        assertTrue(t2.isAlive());
        assertTrue(thread1InSync.get());
        assertTrue(thread2InSync.get());
        
        // Check thread states - at least one should be BLOCKED
        Thread.State t1State = t1.getState();
        Thread.State t2State = t2.getState();
        assertTrue(t1State == Thread.State.BLOCKED || t2State == Thread.State.BLOCKED,
                   "At least one thread should be BLOCKED. T1: " + t1State + ", T2: " + t2State);
        
        // Verify deadlock using ThreadMXBean, counting only the two threads this test started.
        long[] deadlockedThreadIds = deadlockedAmong(t1, t2);
        assertNotNull(deadlockedThreadIds, "Deadlocked threads should be detected");
        assertTrue(deadlockedThreadIds.length >= 2, "At least 2 threads should be deadlocked");
        
        // Clean up by interrupting threads
        t1.interrupt();
        t2.interrupt();
    }

    /**
     * Test that demonstrates how multiple pairs of threads can create multiple deadlocks
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMultipleDeadlocks() throws InterruptedException {
        final int NUM_PAIRS = 3;
        Thread[] threads = new Thread[NUM_PAIRS * 2];
        
        for (int i = 0; i < NUM_PAIRS; i++) {
            // Create a new pair of locks for each pair of threads
            final Object lock1 = new Object();
            final Object lock2 = new Object();
            final int pairIndex = i;
            
            threads[i * 2] = new Thread(() -> {
                synchronized (lock1) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    synchronized (lock2) {
                        // Should not reach here
                    }
                }
            }, "DeadlockPair-" + pairIndex + "-Thread-1");
            
            threads[i * 2 + 1] = new Thread(() -> {
                synchronized (lock2) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    synchronized (lock1) {
                        // Should not reach here
                    }
                }
            }, "DeadlockPair-" + pairIndex + "-Thread-2");
        }
        
        // Start all threads
        for (Thread t : threads) {
            t.start();
        }
        
        // Wait for every pair to close its cycle. A flat sleep here made the test flaky: on a loaded
        // machine 500ms is not always enough for all six threads to reach their second monitor.
        awaitDeadlockAmong(NUM_PAIRS * 2, threads);
        
        // Count only the threads this test started -- findDeadlockedThreads() is JVM-wide, and the
        // other methods here leak deadlocked threads that stay blocked for the rest of the run.
        long[] deadlockedThreadIds = deadlockedAmong(threads);
        
        assertNotNull(deadlockedThreadIds, "Deadlocked threads should be detected");
        assertTrue(deadlockedThreadIds.length >= NUM_PAIRS * 2, 
                   "At least " + (NUM_PAIRS * 2) + " threads should be deadlocked, but found " + 
                   deadlockedThreadIds.length);
        
        // Clean up
        for (Thread t : threads) {
            t.interrupt();
        }
    }

    /**
     * Returns the ids of the given threads that the JVM reports as deadlocked, or null if none are.
     * <p>
     * {@link ThreadMXBean#findDeadlockedThreads()} scans every thread in the JVM, and several tests
     * here deliberately leave threads deadlocked for good -- a thread blocked entering a monitor
     * cannot be interrupted out of it. Filtering to the threads a test actually started keeps its
     * assertions about its own behaviour.
     *
     * @param threads the threads to consider.
     * @return the deadlocked subset of their ids, or null if none of them are deadlocked.
     */
    private static long[] deadlockedAmong(Thread... threads) {
        long[] deadlocked = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        if (deadlocked == null) {
            return null;
        }
        Set<Long> ours = Arrays.stream(threads).map(Thread::threadId).collect(Collectors.toSet());
        long[] mine = Arrays.stream(deadlocked).filter(ours::contains).toArray();
        return mine.length == 0 ? null : mine;
    }

    /**
     * Polls until at least {@code expected} of the given threads are deadlocked, or the deadline passes.
     * <p>
     * Deadlock formation is not instantaneous: each thread has to be scheduled, take its first monitor
     * and then block on the second. Waiting for the condition rather than sleeping a guessed interval
     * keeps the test both fast and reliable under load.
     * <p>
     * The poll interval is deliberately not tight. {@link ThreadMXBean#findDeadlockedThreads()} walks
     * every thread in the JVM, so calling it in a hot loop is expensive once a full suite run has
     * accumulated threads -- enough to exhaust the caller's own {@code @Timeout} before the cycle is
     * ever reported. The budget likewise stays well inside that timeout.
     *
     * @param expected how many of the threads should end up deadlocked.
     * @param threads  the threads to watch.
     * @throws InterruptedException if this thread is interrupted while polling.
     */
    private static void awaitDeadlockAmong(int expected, Thread... threads) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            long[] deadlocked = deadlockedAmong(threads);
            if (deadlocked != null && deadlocked.length >= expected) {
                return;
            }
            Thread.sleep(50);
        }
    }
}
