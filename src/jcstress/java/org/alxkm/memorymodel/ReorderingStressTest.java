package org.alxkm.memorymodel;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Instruction reordering, caught in the act.
 * <p>
 * This is the classic Dekker idiom. Two threads each write one field and then read the other:
 * <pre>
 *   actor1:  x = 1;  r1 = y;
 *   actor2:  y = 1;  r2 = x;
 * </pre>
 * Read the source literally and {@code (0, 0)} looks impossible. Whichever store lands first, the other
 * thread's load should see it, so at least one reader should observe a 1.
 * <p>
 * It happens anyway. A store may sit in the writing core's store buffer while that core proceeds to its
 * load, so both loads can execute before either store becomes visible -- a StoreLoad reordering. The JMM
 * permits it because there is no happens-before edge between the two threads, and x86 hardware performs
 * it. This is the concrete answer to "what does volatile actually prevent": making the fields volatile
 * forbids this outcome, at the cost of the StoreLoad barrier that enforces it.
 *
 * <h2>Why this is not a unit test</h2>
 * It was tried. A plain thread-per-iteration probe found zero {@code (0, 0)} outcomes in 20,000 runs,
 * and a barrier-synchronised loop found zero in 500,000. The reason is structural rather than bad luck:
 * any handshake strong enough to line the two threads up is itself a memory barrier, and it drains the
 * store buffer that produces the effect. jcstress avoids that by spinning the actors without
 * synchronisation and sampling the results in bulk.
 */
public class ReorderingStressTest {

    /**
     * Plain fields: {@code (0, 0)} is permitted, and on x86 it is observed.
     */
    @JCStressTest
    @Description("Two plain writes followed by two plain reads; (0, 0) proves a StoreLoad reordering")
    @Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Both loads saw the other store")
    @Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "actor1 ran first")
    @Outcome(id = "1, 0", expect = Expect.ACCEPTABLE, desc = "actor2 ran first")
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE_INTERESTING,
            desc = "Reordering: neither load saw the other store, which the source order forbids")
    @State
    public static class PlainFields {
        int x;
        int y;

        @Actor
        public void actor1(II_Result r) {
            x = 1;
            r.r1 = y;
        }

        @Actor
        public void actor2(II_Result r) {
            y = 1;
            r.r2 = x;
        }
    }

    /**
     * The same test with volatile fields: {@code (0, 0)} becomes forbidden.
     * <p>
     * Marking it {@link Expect#FORBIDDEN} means jcstress fails the run if it ever shows up. That turns
     * the guarantee into something checked rather than asserted in prose.
     */
    @JCStressTest
    @Description("The same idiom with volatile fields, where sequential consistency forbids (0, 0)")
    @Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Both loads saw the other store")
    @Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "actor1 ran first")
    @Outcome(id = "1, 0", expect = Expect.ACCEPTABLE, desc = "actor2 ran first")
    @Outcome(id = "0, 0", expect = Expect.FORBIDDEN,
            desc = "Volatile accesses are sequentially consistent; this must never happen")
    @State
    public static class VolatileFields {
        volatile int x;
        volatile int y;

        @Actor
        public void actor1(II_Result r) {
            x = 1;
            r.r1 = y;
        }

        @Actor
        public void actor2(II_Result r) {
            y = 1;
            r.r2 = x;
        }
    }
}
