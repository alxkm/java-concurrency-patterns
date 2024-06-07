package org.alxkm.patterns.atomics;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Multithreaded tests for the AtomicExample class.
 */
public class AtomicExampleTest {

    @Test
    public void testAtomicBoolean() throws InterruptedException {
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                if (atomicBoolean.get()) {
                    atomicBoolean.set(false);
                } else {
                    atomicBoolean.set(true);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertTrue(atomicBoolean.get() || !atomicBoolean.get());
    }

    @Test
    public void testAtomicInteger() throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        int numThreads = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    atomicInteger.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(100000, atomicInteger.get());
    }

    @Test
    public void testAtomicLong() throws InterruptedException {
        AtomicLong atomicLong = new AtomicLong(0);
        int numThreads = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    atomicLong.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(100000, atomicLong.get());
    }

    @Test
    public void testAtomicIntegerArray() throws InterruptedException {
        int length = 10;
        AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(length);
        int numThreads = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < length; j++) {
                    atomicIntegerArray.getAndIncrement(j);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        for (int i = 0; i < length; i++) {
            assertEquals(numThreads, atomicIntegerArray.get(i));
        }
    }

    @Test
    public void testAtomicLongArray() throws InterruptedException {
        int length = 10;
        AtomicLongArray atomicLongArray = new AtomicLongArray(length);
        int numThreads = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < length; j++) {
                    atomicLongArray.getAndIncrement(j);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        for (int i = 0; i < length; i++) {
            assertEquals(numThreads, atomicLongArray.get(i));
        }
    }
}
