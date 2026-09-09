package org.alxkm.memorymodel;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * Unsafe publication, caught in the act: a reader that sees the reference but not the constructor.
 * <p>
 * {@code holder = new Holder(42)} is three steps -- allocate, write the field, publish the reference --
 * and without an ordering edge another thread may observe them out of order. The reader then holds a
 * perfectly non-null object whose field is still 0, even though nothing ever mutates it afterwards.
 * <p>
 * {@link SafePublicationExample} shows the shape of this in plain Java but cannot reliably reproduce it:
 * the window is a few instructions wide. Here the outcome is enumerated instead of hoped for, and the
 * safe variants are marked {@link Expect#FORBIDDEN} so the run fails if a guarantee is ever violated.
 */
public class SafePublicationStressTest {

    /** Reader observed nothing yet -- the race did not occur on this sample. */
    private static final int NOT_PUBLISHED = -1;

    static class Holder {
        int value;

        Holder(int value) {
            this.value = value;
        }
    }

    static class FinalHolder {
        final int value;

        FinalHolder(int value) {
            this.value = value;
        }
    }

    /**
     * Published through a plain field: the reader may see a half-built object.
     */
    @JCStressTest
    @Description("Publishing through a plain field lets a reader see the reference before the field write")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Reader ran before publication")
    @Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Reader saw a fully constructed object")
    @Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING,
            desc = "Unsafe publication: non-null reference, field still at its default")
    @State
    public static class PlainField {
        Holder holder;

        @Actor
        public void writer() {
            holder = new Holder(42);
        }

        @Actor
        public void reader(I_Result r) {
            Holder local = holder;
            r.r1 = (local == null) ? NOT_PUBLISHED : local.value;
        }
    }

    /**
     * The same handover with a final field: seeing 0 becomes forbidden.
     * <p>
     * The final-field guarantee is what makes immutable objects safe to hand around with no
     * synchronisation at all -- note that the publishing field here is still plain.
     */
    @JCStressTest
    @Description("A final field must be visible to any thread that sees the reference")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Reader ran before publication")
    @Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Reader saw a fully constructed object")
    @Outcome(id = "0", expect = Expect.FORBIDDEN,
            desc = "The final-field guarantee forbids observing the default here")
    @State
    public static class FinalField {
        FinalHolder holder;

        @Actor
        public void writer() {
            holder = new FinalHolder(42);
        }

        @Actor
        public void reader(I_Result r) {
            FinalHolder local = holder;
            r.r1 = (local == null) ? NOT_PUBLISHED : local.value;
        }
    }

    /**
     * A mutable object published through a volatile field: also safe, by a different rule.
     * <p>
     * Here the guarantee comes from the volatile write rather than from the object's shape, which is why
     * this works for classes whose fields cannot be final.
     */
    @JCStressTest
    @Description("A volatile write publishes the object it points at, final fields or not")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Reader ran before publication")
    @Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Reader saw a fully constructed object")
    @Outcome(id = "0", expect = Expect.FORBIDDEN,
            desc = "The volatile write happens-before the read that sees it")
    @State
    public static class VolatileField {
        volatile Holder holder;

        @Actor
        public void writer() {
            holder = new Holder(42);
        }

        @Actor
        public void reader(I_Result r) {
            Holder local = holder;
            r.r1 = (local == null) ? NOT_PUBLISHED : local.value;
        }
    }
}
