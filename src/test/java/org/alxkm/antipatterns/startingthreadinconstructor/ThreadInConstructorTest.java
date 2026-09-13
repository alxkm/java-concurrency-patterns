package org.alxkm.antipatterns.startingthreadinconstructor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for starting a thread inside a constructor, and the two ways out.
 * <p>
 * The state of a freshly constructed object says it plainly: a Thread that has not been started is in
 * state NEW, so if construction alone leaves it in any other state the constructor started it.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ThreadInConstructorTest {

    /**
     * Where the escaped thread records what it saw.
     * <p>
     * Static on purpose, and the reason is the finding itself. Subclass field initialisers run after
     * the superclass constructor, so an instance field here would still be null when the escaped
     * thread arrives. The first version of this test used an instance AtomicInteger and the thread
     * died on a NullPointerException before it could record anything, which is the same defect
     * wearing a different hat.
     */
    private static final AtomicInteger OBSERVED_VALUE = new AtomicInteger(-1);

    private static final CountDownLatch RAN = new CountDownLatch(1);

    @Test
    void constructorLeavesTheThreadAlreadyStarted() throws InterruptedException {
        ThreadInConstructor thread = new ThreadInConstructor("started from the constructor");

        assertNotEquals(Thread.State.NEW, thread.getState(),
                "the constructor called start(), so the object is running before the caller has it");

        thread.join(TimeUnit.SECONDS.toMillis(5));
    }

    @Test
    void startingOutsideTheConstructorLeavesItUnstarted() throws InterruptedException {
        ThreadStartedOutsideConstructor thread =
                new ThreadStartedOutsideConstructor("started by the caller");

        assertEquals(Thread.State.NEW, thread.getState(),
                "construction should not start anything; the caller decides when");

        thread.start();
        thread.join(TimeUnit.SECONDS.toMillis(5));
        assertEquals(Thread.State.TERMINATED, thread.getState());
    }

    /**
     * Why the escape matters, rather than merely being untidy.
     * <p>
     * A superclass constructor runs before the subclass constructor body and before the subclass field
     * initialisers. If the superclass starts a thread, that thread reaches the object while the
     * subclass is still half built, and a subclass overriding run() reads its own fields at their
     * defaults. The subclass has done nothing wrong and cannot defend itself: there is no point in its
     * own code where it could have run any earlier.
     */
    @Test
    void aSubclassIsObservedBeforeItsFieldsAreSet() throws InterruptedException {
        EscapingSubclass subclass = new EscapingSubclass();

        assertTrue(RAN.await(5, TimeUnit.SECONDS), "the escaped thread should have run");
        subclass.join(TimeUnit.SECONDS.toMillis(5));

        assertEquals(0, OBSERVED_VALUE.get(),
                "the thread read the subclass field before the constructor had assigned it");
        assertEquals(42, subclass.value, "and the field is correct once construction has finished");
    }

    /**
     * The factory approach removes the choice: the constructor is unreachable, so nobody can build one
     * without going through the method that also starts it.
     */
    @Test
    void factoryHidesTheConstructor() {
        for (Constructor<?> constructor : ThreadUsingFactory.class.getDeclaredConstructors()) {
            assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                    "every constructor should be private so construction goes through the factory");
        }
    }

    @Test
    void factoryCreatesAndStartsTheWork() {
        assertNotNull(ThreadUsingFactory.createAndStart("started by the factory"),
                "the factory should hand back the object it started");
    }

    /** A subclass of the offending class, used to show what its constructor exposes. */
    private static final class EscapingSubclass extends ThreadInConstructor {

        /** Left at its default while the escaped thread is already running. */
        private int value;

        @SuppressWarnings("this-escape")
        private EscapingSubclass() {
            super("subclass");

            // Wait until the escaped thread has read the field, so this test asserts an ordering
            // rather than the outcome of a race. The window is real either way: without the latch it
            // is a few nanoseconds wide and the assignment usually wins, which is precisely what makes
            // this class of bug so hard to catch. A real subclass doing real work in its constructor
            // loses the same race far more often.
            awaitQuietly(RAN);
            value = 42;
        }

        @Override
        public void run() {
            OBSERVED_VALUE.set(value);
            RAN.countDown();
        }
    }

    /**
     * Awaits a latch without forcing a checked exception on the caller.
     *
     * @param latch the latch to wait on.
     */
    private static void awaitQuietly(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the escaped thread never ran");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
