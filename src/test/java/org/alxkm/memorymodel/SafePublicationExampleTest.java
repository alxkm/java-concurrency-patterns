package org.alxkm.memorymodel;

import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every safe idiom must publish the constructor's writes along with the reference, on every run.
 * <p>
 * Repeated rather than run once: each of these has the reader spinning concurrently with the writer, so
 * a single pass proves very little. Repetition is still not a substitute for jcstress -- it widens the
 * window rather than exploring the interleavings -- but it does turn a formality into a real exercise
 * of the handover.
 * <p>
 * The unsafe variant is deliberately absent. It would pass here essentially always, since the window
 * between the field write and the reference write is a few instructions wide, and a test that passes
 * against broken code is worse than no test. It belongs in the jcstress source set, which is built to
 * find exactly that.
 */
class SafePublicationExampleTest {

    private static final int PAYLOAD = 42;

    @RepeatedTest(50)
    void finalFieldPublishesWithoutSynchronisation() throws InterruptedException {
        assertEquals(PAYLOAD, new SafePublicationExample().publishedByFinalField(PAYLOAD),
                "a final field must be visible to any thread that sees the reference");
    }

    @RepeatedTest(50)
    void volatileFieldPublishes() throws InterruptedException {
        assertEquals(PAYLOAD, new SafePublicationExample().publishedByVolatileField(PAYLOAD),
                "a volatile write must publish the object it points at");
    }

    @RepeatedTest(50)
    void atomicReferencePublishes() throws InterruptedException {
        assertEquals(PAYLOAD, new SafePublicationExample().publishedByAtomicReference(PAYLOAD),
                "AtomicReference.set must publish the object it points at");
    }

    @RepeatedTest(5)
    void staticInitialiserPublishes() throws InterruptedException {
        assertEquals(99, new SafePublicationExample().publishedByStaticInitialiser(),
                "class initialisation must publish to every thread that touches the class");
    }
}
