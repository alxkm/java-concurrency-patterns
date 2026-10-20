package org.alxkm.memorymodel;

import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A release/acquire handover must deliver the payload on every run.
 * <p>
 * Only the guaranteed half is asserted. The other half, that release/acquire does not stop Dekker's
 * {@code (0, 0)}, is a reordering a unit test cannot line up without hiding it, so it lives in
 * {@code ReleaseAcquireStressTest} in the jcstress source set. Repeated for the same reason as
 * {@link SafePublicationExampleTest}: the reader spins concurrently with the writer, and one pass would
 * exercise the handover once.
 */
class ReleaseAcquireExampleTest {

    @RepeatedTest(50)
    void readerThatSeesTheFlagSeesThePayload() throws InterruptedException {
        assertEquals(42, new ReleaseAcquireExample().handOver(42),
                "an acquire read that sees the release write must also see what was written before it");
    }
}
