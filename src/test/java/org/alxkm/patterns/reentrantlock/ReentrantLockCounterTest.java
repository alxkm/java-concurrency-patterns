package org.alxkm.patterns.reentrantlock;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReentrantLockCounterTest {
    private static final int THREADS = 8;
    private static final int INCREMENTS = 25_000;

    @Test
    void countsEveryIncrementUnderContention() throws InterruptedException {
        ReentrantLockCounter counter = new ReentrantLockCounter();

        Concurrently.run(THREADS, INCREMENTS, counter::incrementCounter);

        assertEquals(THREADS * INCREMENTS, counter.getCounter());
    }

    @Test
    void startsAtZero() {
        assertEquals(0, new ReentrantLockCounter().getCounter());
    }
}
