package org.alxkm.memorymodel;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Each edge here is a guarantee the memory model makes, so each of these assertions must hold on every
 * run and every JVM. That is what separates this class from {@link VisibilityExampleTest}, where the
 * interesting case is explicitly unspecified.
 */
class HappensBeforeExampleTest {

    private static final int PAYLOAD = 4242;

    @Test
    void threadStartPublishesEarlierWrites() throws InterruptedException {
        assertEquals(PAYLOAD, new HappensBeforeExample().publishedByThreadStart(PAYLOAD),
                "writes before Thread.start() must be visible to the started thread");
    }

    @Test
    void threadJoinPublishesTheThreadsWrites() throws InterruptedException {
        assertEquals(PAYLOAD, new HappensBeforeExample().publishedByThreadJoin(PAYLOAD),
                "writes inside a thread must be visible once join() returns");
    }

    /**
     * Repeated because this is the one edge with a genuine race in it: the reader spins concurrently
     * with the writer rather than being ordered by start/join. If the volatile write did not publish
     * the plain payload alongside itself, a reader could see the flag set and the payload still zero,
     * and repetition is what gives that window a chance to be hit.
     */
    @RepeatedTest(50)
    void volatileWritePublishesThePayloadWrittenBeforeIt() throws InterruptedException {
        assertEquals(PAYLOAD, new HappensBeforeExample().publishedByVolatileFlag(PAYLOAD),
                "a volatile write must publish everything written before it, not just itself");
    }

    @Test
    void lockReleasePublishesToTheNextAcquirer() throws InterruptedException {
        assertEquals(PAYLOAD, new HappensBeforeExample().publishedByLock(PAYLOAD),
                "unlocking must publish to whoever acquires the same lock next");
    }
}
