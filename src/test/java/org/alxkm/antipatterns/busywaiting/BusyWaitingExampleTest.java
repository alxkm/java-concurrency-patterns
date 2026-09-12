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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
     * Busy waiting burns a core; blocking does not.
     *
     * The previous version of this test set an absolute floor: spin for 100ms of wall time and assert
     * more than 50ms of CPU was consumed. That is a coin flip by construction, and it failed at 46ms.
     * Comparing the two strategies over the same interval measures the actual difference instead, and
     * the gap is orders of magnitude rather than a few percent.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testBusyWaitingConsumesFarMoreCpuThanBlocking() throws Exception {
        long busyCpuNanos = cpuTimeOf("spin", () -> {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS);
            while (System.nanoTime() < deadline) {
                // Busy wait, which is the antipattern being measured.
            }
        });

        long blockedCpuNanos = cpuTimeOf("sleep", () -> {
            try {
                Thread.sleep(WAIT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        System.out.printf("over %dms: spinning used %.1fms of CPU, sleeping used %.1fms%n",
                WAIT_MILLIS, busyCpuNanos / 1e6, blockedCpuNanos / 1e6);

        // A parked thread uses essentially no CPU, so even a very loose factor holds comfortably.
        assertTrue(busyCpuNanos > blockedCpuNanos * 10,
                "spinning should cost far more CPU than blocking, but used " + busyCpuNanos
                        + "ns against " + blockedCpuNanos + "ns");
    }

    /** How long each strategy waits, in milliseconds. */
    private static final long WAIT_MILLIS = 200;

    /**
     * Runs the body on its own thread and reports the CPU time that thread consumed.
     *
     * @param name the thread name.
     * @param body the work to measure.
     * @return CPU time in nanoseconds.
     * @throws InterruptedException if this thread is interrupted while joining.
     */
    private static long cpuTimeOf(String name, Runnable body) throws InterruptedException {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadCpuTimeSupported(), "per-thread CPU time is not available here");

        AtomicLong consumed = new AtomicLong();
        Thread thread = new Thread(() -> {
            long id = Thread.currentThread().threadId();
            long before = bean.getThreadCpuTime(id);
            body.run();
            consumed.set(bean.getThreadCpuTime(id) - before);
        }, name);
        thread.start();
        thread.join();
        return consumed.get();
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