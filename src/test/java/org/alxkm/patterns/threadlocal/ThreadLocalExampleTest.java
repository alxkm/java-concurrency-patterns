package org.alxkm.patterns.threadlocal;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadLocalExampleTest {

    /**
     * Verifies that ThreadLocal really does give every thread its own copy: each of the threads below writes
     * a distinct value and then reads the variable back, and every one of them must see what it wrote rather
     * than another thread's value.
     * <p>
     * The values are returned to the test thread rather than asserted inside the worker threads. An
     * AssertionError thrown on a worker only kills that worker -- JUnit never sees it -- so a test that
     * asserts in the threads it spawns passes whether the code is right or wrong.
     */
    @Test
    void eachThreadSeesOnlyItsOwnValue() throws Exception {
        int threads = 8;
        AtomicInteger nextValue = new AtomicInteger();

        List<Integer> observed = Concurrently.collect(threads, () -> {
            int mine = nextValue.incrementAndGet();
            ThreadLocalExample.THREAD_LOCAL.set(mine);
            Thread.yield(); // give the other threads every chance to clobber the value
            int seen = ThreadLocalExample.THREAD_LOCAL.get();
            return seen == mine ? mine : -1;
        });

        assertEquals(threads, observed.size());
        assertEquals(threads, observed.stream().filter(value -> value > 0).count(),
                "every thread must read back the value it wrote, but some saw another thread's");
        assertEquals(threads, observed.stream().distinct().count(),
                "each thread should have written a distinct value");
    }

    /**
     * A thread that never writes sees the initial value supplied to {@code ThreadLocal.withInitial}, not
     * whatever the previous user of that thread left behind.
     */
    @Test
    void unwrittenThreadSeesTheInitialValue() throws Exception {
        List<Integer> observed = Concurrently.collect(4, ThreadLocalExample.THREAD_LOCAL::get);

        assertEquals(List.of(0, 0, 0, 0), observed);
    }
}
