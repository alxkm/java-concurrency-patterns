package org.alxkm.memorymodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two halves of this example need different kinds of test, and the difference is the lesson.
 * <p>
 * The volatile case is a guarantee, so it is asserted. The plain case is explicitly <em>unspecified</em>
 * -- the JMM permits the reader to miss the write forever and permits it to see the write -- so there is
 * nothing to assert without writing a test that pins down behaviour the platform never promised. It is
 * exercised instead, to prove it terminates rather than hanging the suite, and what it observed is
 * reported.
 */
class VisibilityExampleTest {

    @Test
    void volatileWriteIsAlwaysObserved() throws InterruptedException {
        assertTrue(new VisibilityExample().volatileReaderObservesWrite(),
                "a volatile write happens-before the reader's next read of it, so the reader must stop");
    }

    /**
     * Runs the unsynchronised case and records the outcome without asserting it.
     * <p>
     * On a JIT-compiled JVM the reader almost always spins forever, because the compiler may hoist a
     * read that nothing in the loop can change. Asserting that would be asserting a permission rather
     * than a promise, and would fail under {@code -Xint} while telling us nothing true about the model.
     */
    @Test
    void plainWriteCarriesNoGuaranteeEitherWay() throws InterruptedException {
        boolean observed = new VisibilityExample().plainReaderObservesWrite();

        System.out.println("plain (non-volatile) field: reader observed the write = " + observed
                + (observed
                ? "  -- permitted, but do not rely on it"
                : "  -- the reader is still spinning; this is why volatile exists"));
    }
}
