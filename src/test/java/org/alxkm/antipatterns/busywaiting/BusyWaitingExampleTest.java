package org.alxkm.antipatterns.busywaiting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusyWaitingExampleTest {

    /**
     * Helper method to set the private flag field using reflection
     */
    private void setFlag(BusyWaitingExample example, boolean value) throws NoSuchFieldException, IllegalAccessException {
        Field flagField = BusyWaitingExample.class.getDeclaredField("flag");
        flagField.setAccessible(true);
        flagField.set(example, value);
    }

    /**
     * Helper method to get the private flag field using reflection
     */
    private boolean getFlag(BusyWaitingExample example) throws NoSuchFieldException, IllegalAccessException {
        Field flagField = BusyWaitingExample.class.getDeclaredField("flag");
        flagField.setAccessible(true);
        return (boolean) flagField.get(example);
    }

    /**
     * Demonstrates that busy waiting consumes excessive CPU time
     */
    //@Test
    //@Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testBusyWaitingConsumesCPU() throws Exception {
        BusyWaitingExample example = new BusyWaitingExample();
        AtomicLong cpuTimeSpent = new AtomicLong(0);
        CountDownLatch threadStarted = new CountDownLatch(1);
        AtomicBoolean measurementComplete = new AtomicBoolean(false);

        Thread busyWaitThread = new Thread(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            long threadId = Thread.currentThread().getId();
            threadStarted.countDown();
            
            // Measure CPU time before busy waiting
            long startCpuTime = threadMXBean.getThreadCpuTime(threadId);
            
            // Perform busy waiting for a short period
            long startTime = System.currentTimeMillis();
            try {
                while (System.currentTimeMillis() - startTime < 100 && !getFlag(example)) {
                    // Busy wait
                }
            } catch (Exception e) {
                // Handle reflection exception
            }
            
            // Measure CPU time after busy waiting
            long endCpuTime = threadMXBean.getThreadCpuTime(threadId);
            cpuTimeSpent.set(endCpuTime - startCpuTime);
            measurementComplete.set(true);
        });

        busyWaitThread.start();
        threadStarted.await();
        
        // Let it busy wait for a bit
        Thread.sleep(150);
        
        // Stop the busy waiting
        setFlag(example, true);
        busyWaitThread.join();

        // Verify that significant CPU time was consumed (more than 50% of elapsed time)
        assertTrue(measurementComplete.get());
        long cpuTimeMs = cpuTimeSpent.get() / 1_000_000; // Convert nanoseconds to milliseconds
        assertTrue(cpuTimeMs > 50, "Busy waiting should consume significant CPU time, but consumed only " + cpuTimeMs + "ms");
    }

    /**
     * Demonstrates that busy waiting blocks the thread from doing other work
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void testBusyWaitingBlocksThread() throws Exception {
        BusyWaitingExample example = new BusyWaitingExample();
        AtomicBoolean workStarted = new AtomicBoolean(false);
        AtomicBoolean workCompleted = new AtomicBoolean(false);
        CountDownLatch busyWaitStarted = new CountDownLatch(1);

        Thread workerThread = new Thread(() -> {
            busyWaitStarted.countDown();
            workStarted.set(true);
            example.doWork(); // This will busy wait
            workCompleted.set(true);
        });

        workerThread.start();
        busyWaitStarted.await();
        
        // Verify thread is blocked in busy waiting
        Thread.sleep(100);
        assertTrue(workStarted.get());
        assertFalse(workCompleted.get());
        
        // Release the busy wait
        setFlag(example, true);
        workerThread.join();
        
        // Verify work completed after flag was set
        assertTrue(workCompleted.get());
    }

    /**
     * Demonstrates the anti-pattern: multiple threads busy waiting cause high CPU usage
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testMultipleThreadsBusyWaitingCauseHighCPU() throws Exception {
        final int NUM_THREADS = 4;
        BusyWaitingExample example = new BusyWaitingExample();
        Thread[] threads = new Thread[NUM_THREADS];
        CountDownLatch allThreadsStarted = new CountDownLatch(NUM_THREADS);
        
        // Start multiple threads all busy waiting
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                allThreadsStarted.countDown();
                example.doWork();
            });
            threads[i].start();
        }
        
        // Wait for all threads to start busy waiting
        allThreadsStarted.await();
        
        // Let them busy wait for a bit
        Thread.sleep(100);
        
        // Check that all threads are still alive (stuck in busy wait)
        for (Thread t : threads) {
            assertTrue(t.isAlive());
        }
        
        // Release all threads
        setFlag(example, true);
        
        // Wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }
    }
}