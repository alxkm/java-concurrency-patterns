package org.alxkm.antipatterns.deadlock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
        Thread.sleep(200); // Give time for deadlock to occur
        
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
        
        // Verify deadlock using ThreadMXBean
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
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
        
        // Wait a bit for deadlocks to form
        Thread.sleep(500);
        
        // Check for deadlocks
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
        
        assertNotNull(deadlockedThreadIds, "Deadlocked threads should be detected");
        assertTrue(deadlockedThreadIds.length >= NUM_PAIRS * 2, 
                   "At least " + (NUM_PAIRS * 2) + " threads should be deadlocked, but found " + 
                   deadlockedThreadIds.length);
        
        // Clean up
        for (Thread t : threads) {
            t.interrupt();
        }
    }
}