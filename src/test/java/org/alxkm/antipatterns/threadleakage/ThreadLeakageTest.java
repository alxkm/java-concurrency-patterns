package org.alxkm.antipatterns.threadleakage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThreadLeakageTest {

    /**
     * Demonstrates that creating threads without limit causes thread leakage
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testThreadLeakage() throws InterruptedException {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int initialThreadCount = threadMXBean.getThreadCount();
        
        AtomicInteger threadsCreated = new AtomicInteger(0);
        AtomicBoolean stopCreating = new AtomicBoolean(false);
        
        // Modified version that we can control
        Thread leakingThread = new Thread(() -> {
            while (!stopCreating.get() && threadsCreated.get() < 50) {
                new Thread(() -> {
                    try {
                        Thread.sleep(5000); // Keep threads alive longer
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                threadsCreated.incrementAndGet();
                
                try {
                    Thread.sleep(50); // Small delay between thread creation
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        leakingThread.start();
        
        // Let it create some threads
        Thread.sleep(1000);
        stopCreating.set(true);
        leakingThread.join();
        
        // Check thread count increased significantly
        int currentThreadCount = threadMXBean.getThreadCount();
        int threadsLeaked = currentThreadCount - initialThreadCount;
        
        assertTrue(threadsLeaked > 20, 
                   "Thread leakage should cause significant increase in thread count. " +
                   "Initial: " + initialThreadCount + ", Current: " + currentThreadCount + 
                   ", Leaked: " + threadsLeaked);
        
        // Verify most created threads are still alive (leaked)
        assertTrue(threadsCreated.get() > 20, "Should have created many threads");
    }

    /**
     * Demonstrates memory impact of thread leakage
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testThreadLeakageMemoryImpact() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Request garbage collection
        Thread.sleep(100);
        
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        AtomicBoolean stopCreating = new AtomicBoolean(false);
        AtomicInteger threadsCreated = new AtomicInteger(0);
        
        // Create threads with larger stack allocations
        Thread leakingThread = new Thread(() -> {
            while (!stopCreating.get() && threadsCreated.get() < 30) {
                Thread t = new Thread(() -> {
                    byte[] data = new byte[1024 * 10]; // 10KB per thread
                    try {
                        Thread.sleep(10000); // Keep alive for measurement
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                t.start();
                threadsCreated.incrementAndGet();
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        leakingThread.start();
        Thread.sleep(2000);
        stopCreating.set(true);
        leakingThread.join();
        
        runtime.gc();
        Thread.sleep(100);
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = currentMemory - initialMemory;
        
        // Memory should have increased due to thread overhead
        assertTrue(memoryIncrease > 0, 
                   "Thread leakage should increase memory usage. Increase: " + 
                   memoryIncrease + " bytes");
    }

    /**
     * Demonstrates that leaked threads prevent JVM shutdown
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testLeakedThreadsPreventShutdown() throws InterruptedException {
        AtomicInteger nonDaemonThreadsCreated = new AtomicInteger(0);
        CountDownLatch threadCreated = new CountDownLatch(1);
        
        // Create a non-daemon thread that would prevent JVM shutdown
        Thread leaker = new Thread(() -> {
            Thread nonDaemonThread = new Thread(() -> {
                try {
                    Thread.sleep(60000); // Sleep for a long time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            nonDaemonThread.setDaemon(false); // Non-daemon thread
            nonDaemonThread.start();
            nonDaemonThreadsCreated.incrementAndGet();
            threadCreated.countDown();
        });
        
        leaker.start();
        leaker.join();
        threadCreated.await();
        
        // Verify non-daemon thread was created
        assertEquals(1, nonDaemonThreadsCreated.get(), 
                     "Should have created a non-daemon thread");
        
        // In a real application, this thread would prevent JVM shutdown
        // For testing, we just verify it was created
    }

    /**
     * Demonstrates resource exhaustion from thread leakage
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testResourceExhaustion() {
        AtomicInteger threadsCreated = new AtomicInteger(0);
        AtomicBoolean outOfResourcesDetected = new AtomicBoolean(false);
        AtomicBoolean stopCreating = new AtomicBoolean(false);
        
        Thread leaker = new Thread(() -> {
            while (!stopCreating.get() && !outOfResourcesDetected.get()) {
                try {
                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(30000); // Keep threads alive
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    t.start();
                    threadsCreated.incrementAndGet();
                    
                    // Stop after creating many threads to avoid system issues
                    if (threadsCreated.get() >= 100) {
                        break;
                    }
                } catch (OutOfMemoryError e) {
                    // This might happen if too many threads are created
                    outOfResourcesDetected.set(true);
                    break;
                }
                
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        leaker.start();
        
        try {
            leaker.join(5000); // Wait max 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        stopCreating.set(true);
        
        // Verify significant number of threads were created
        assertTrue(threadsCreated.get() > 50 || outOfResourcesDetected.get(), 
                   "Should have created many threads or detected resource exhaustion. " +
                   "Created: " + threadsCreated.get());
    }
}