package org.alxkm.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ThreadDumpExample}.
 *
 * These pin down the distinction the class exists to teach: contention on a monitor shows up as
 * BLOCKED, while everything in java.util.concurrent parks the thread and shows up as WAITING. Reading
 * a dump for BLOCKED alone therefore misses every ReentrantLock in the system.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ThreadDumpExampleTest {

    @Test
    void contendingForAMonitorShowsAsBlocked() throws InterruptedException {
        Object monitor = new Object();
        CountDownLatch held = new CountDownLatch(1);
        Thread holder = daemon("holder", () -> {
            synchronized (monitor) {
                held.countDown();
                park();
            }
        });
        held.await();

        Thread contender = daemon("contender", () -> {
            synchronized (monitor) {
                throw new IllegalStateException("unreachable");
            }
        });
        awaitState(contender, Thread.State.BLOCKED);

        Map<String, Thread.State> states = ThreadDumpExample.statesOf(contender);
        assertEquals(Thread.State.BLOCKED, states.get("contender"));

        String dump = ThreadDumpExample.dump(contender);
        assertTrue(dump.contains("BLOCKED"), dump);
        assertTrue(dump.contains("held by \"holder\""), "the dump must name the owner:\n" + dump);

        holder.interrupt();
    }

    @Test
    void waitingForAReentrantLockShowsAsWaitingNotBlocked() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch held = new CountDownLatch(1);
        Thread holder = daemon("lock-holder", () -> {
            lock.lock();
            try {
                held.countDown();
                park();
            } finally {
                lock.unlock();
            }
        });
        held.await();

        Thread contender = daemon("lock-contender", () -> {
            lock.lock();
            lock.unlock();
        });
        awaitState(contender, Thread.State.WAITING);

        assertEquals(Thread.State.WAITING, ThreadDumpExample.statesOf(contender).get("lock-contender"),
                "a ReentrantLock parks the thread, so searching a dump for BLOCKED would miss it");

        holder.interrupt();
    }

    @Test
    void waitingForANotificationShowsAsWaiting() throws InterruptedException {
        Object monitor = new Object();
        Thread waiter = daemon("waiter", () -> {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        awaitState(waiter, Thread.State.WAITING);

        assertEquals(Thread.State.WAITING, ThreadDumpExample.statesOf(waiter).get("waiter"));

        waiter.interrupt();
    }

    private static void awaitState(Thread thread, Thread.State wanted) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline && thread.getState() != wanted) {
            Thread.sleep(10);
        }
        assertEquals(wanted, thread.getState(), "thread " + thread.getName() + " never reached " + wanted);
    }

    private static Thread daemon(String name, Runnable body) {
        Thread thread = new Thread(body, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void park() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
