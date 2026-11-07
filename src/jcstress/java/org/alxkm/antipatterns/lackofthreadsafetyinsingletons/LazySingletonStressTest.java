package org.alxkm.antipatterns.lackofthreadsafetyinsingletons;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * The unsafe lazy singleton, caught handing out two instances.
 * <p>
 * {@link UnsafeSingleton#getInstance()} is check-then-act: two threads can both see {@code null} and
 * both construct. The unit test cannot assert that, because the window is a few instructions wide
 * and a single run usually misses it. Here jcstress runs the race millions of times and counts how
 * often two threads come away with different objects.
 * <p>
 * The singleton's state lives in the jcstress state object rather than in a static field, because
 * jcstress makes a fresh state for every sample and a static field could only be initialised once.
 * Each {@code getInstance()} below is the same code as the class it stands for.
 * <p>
 * The arbiter runs after both actors and reports how many distinct instances they got: 1 or 2.
 */
public class LazySingletonStressTest {

    private static final int ONE_INSTANCE = 1;
    private static final int TWO_INSTANCES = 2;

    /**
     * {@link UnsafeSingleton}: a plain field and no lock.
     */
    @JCStressTest
    @Description("An unsynchronised lazy singleton lets two threads construct an instance each")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Both threads got the same instance")
    @Outcome(id = "2", expect = Expect.ACCEPTABLE_INTERESTING,
            desc = "Two instances: both threads saw null and both constructed")
    @State
    public static class Unsynchronized {
        private Object instance;
        private Object seenByActor1;
        private Object seenByActor2;

        @SuppressWarnings("PMD.NonThreadSafeSingleton") // the broken version, kept on purpose
        private Object getInstance() {
            if (instance == null) {
                instance = new Object();
            }
            return instance;
        }

        @Actor
        public void actor1() {
            seenByActor1 = getInstance();
        }

        @Actor
        public void actor2() {
            seenByActor2 = getInstance();
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = seenByActor1 == seenByActor2 ? ONE_INSTANCE : TWO_INSTANCES;
        }
    }

    /**
     * {@link SafeSingleton}: the whole check-then-act under one lock.
     */
    @JCStressTest
    @Description("A synchronized getInstance makes check-then-act atomic")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Both threads got the same instance")
    @Outcome(id = "2", expect = Expect.FORBIDDEN, desc = "The lock forbids a second construction")
    @State
    public static class Synchronized {
        private Object instance;
        private Object seenByActor1;
        private Object seenByActor2;

        private synchronized Object getInstance() {
            if (instance == null) {
                instance = new Object();
            }
            return instance;
        }

        @Actor
        public void actor1() {
            seenByActor1 = getInstance();
        }

        @Actor
        public void actor2() {
            seenByActor2 = getInstance();
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = seenByActor1 == seenByActor2 ? ONE_INSTANCE : TWO_INSTANCES;
        }
    }

    /**
     * {@link DoubleCheckedLockingSingleton}: the lock only on the slow path, the field volatile.
     */
    @JCStressTest
    @Description("Double-checked locking re-checks under the lock, so only one thread constructs")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Both threads got the same instance")
    @Outcome(id = "2", expect = Expect.FORBIDDEN, desc = "The second check under the lock forbids it")
    @State
    public static class DoubleCheckedLocking {
        private volatile Object instance;
        private Object seenByActor1;
        private Object seenByActor2;

        private Object getInstance() {
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) {
                        instance = new Object();
                    }
                }
            }
            return instance;
        }

        @Actor
        public void actor1() {
            seenByActor1 = getInstance();
        }

        @Actor
        public void actor2() {
            seenByActor2 = getInstance();
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = seenByActor1 == seenByActor2 ? ONE_INSTANCE : TWO_INSTANCES;
        }
    }
}
