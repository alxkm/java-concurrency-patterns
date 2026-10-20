package org.alxkm.memorymodel;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * What release/acquire guarantees, and the one thing volatile adds on top.
 * <p>
 * {@link ReleaseAcquireExample} hands a value over through a release/acquire flag. A unit test can only
 * confirm the payload arrives, which it would almost always do even with no ordering at all. Here the
 * outcomes are enumerated instead:
 * <ul>
 *   <li>a handover through plain fields may be seen half done;</li>
 *   <li>through release/acquire it may not, and the run fails if it ever is;</li>
 *   <li>Dekker's idiom through release/acquire still shows {@code (0, 0)}, the StoreLoad reordering that
 *       {@link ReorderingStressTest} shows is forbidden for volatile.</li>
 * </ul>
 * Run just these with {@code ./gradlew jcstress -PjcstressArgs="-t ReleaseAcquire -m quick"}.
 */
public class ReleaseAcquireStressTest {

    private static VarHandle handle(Class<?> owner, String field) {
        try {
            return MethodHandles.lookup().findVarHandle(owner, field, int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Payload, then flag, both plain: seeing the flag says nothing about the payload.
     */
    @JCStressTest
    @Description("A handover through plain fields may be observed half done")
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Reader ran before the handover")
    @Outcome(id = "0, 42", expect = Expect.ACCEPTABLE, desc = "Reader caught the payload but not the flag")
    @Outcome(id = "1, 42", expect = Expect.ACCEPTABLE, desc = "Reader saw the whole handover")
    @Outcome(id = "1, 0", expect = Expect.ACCEPTABLE_INTERESTING,
            desc = "Flag seen, payload not: the plain stores or loads were reordered")
    @State
    public static class PlainHandover {
        int payload;
        int ready;

        @Actor
        public void writer() {
            payload = 42;
            ready = 1;
        }

        @Actor
        public void reader(II_Result r) {
            r.r1 = ready;
            r.r2 = payload;
        }
    }

    /**
     * The same handover with the flag written by setRelease and read by getAcquire: half done is forbidden.
     * <p>
     * The payload stays a plain field. The release keeps the payload write ahead of the flag, the acquire
     * keeps the payload read behind it, and together that is all a one-way handover needs.
     */
    @JCStressTest
    @Description("A release write seen by an acquire read publishes everything written before it")
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Reader ran before the handover")
    @Outcome(id = "0, 42", expect = Expect.ACCEPTABLE, desc = "Reader caught the payload but not the flag")
    @Outcome(id = "1, 42", expect = Expect.ACCEPTABLE, desc = "Reader saw the whole handover")
    @Outcome(id = "1, 0", expect = Expect.FORBIDDEN,
            desc = "Release/acquire forbids seeing the flag without the payload written before it")
    @State
    public static class ReleaseAcquireHandover {
        private static final VarHandle READY = handle(ReleaseAcquireHandover.class, "ready");

        int payload;
        int ready;

        @Actor
        public void writer() {
            payload = 42;
            READY.setRelease(this, 1);
        }

        @Actor
        public void reader(II_Result r) {
            r.r1 = (int) READY.getAcquire(this);
            r.r2 = payload;
        }
    }

    /**
     * Dekker's idiom with release/acquire: {@code (0, 0)} is still allowed.
     * <p>
     * A release orders earlier accesses before the store; an acquire orders later accesses after the
     * load. Neither says anything about a store followed by a load of a different variable, so each
     * store can wait in its core's store buffer while that core reads the other flag. This is the
     * difference between release/acquire and volatile, and the reason mutual exclusion built from two
     * flags needs the full volatile.
     */
    @JCStressTest
    @Description("Release/acquire does not order a store before a later load, so (0, 0) remains possible")
    @Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Both loads saw the other store")
    @Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "actor1 ran first")
    @Outcome(id = "1, 0", expect = Expect.ACCEPTABLE, desc = "actor2 ran first")
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE_INTERESTING,
            desc = "StoreLoad reordering: release/acquire permits what volatile forbids")
    @State
    public static class ReleaseAcquireDekker {
        private static final VarHandle X = handle(ReleaseAcquireDekker.class, "x");
        private static final VarHandle Y = handle(ReleaseAcquireDekker.class, "y");

        int x;
        int y;

        @Actor
        public void actor1(II_Result r) {
            X.setRelease(this, 1);
            r.r1 = (int) Y.getAcquire(this);
        }

        @Actor
        public void actor2(II_Result r) {
            Y.setRelease(this, 1);
            r.r2 = (int) X.getAcquire(this);
        }
    }
}
