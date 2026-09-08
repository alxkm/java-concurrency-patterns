package org.alxkm.antipatterns.lackofthreadsafetyinsingletons;

import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Checks the three thread-safe singletons against the unsafe one.
 * <p>
 * Every thread asks for the instance at the same moment, which is the only time a lazy singleton's
 * initialisation can race. Calling getInstance() twice in a row, as a naive test does, only ever
 * exercises the already-initialised path and passes against every variant here including the
 * broken one.
 */
class SingletonVariantsTest {
    private static final int THREADS = 64;

    private static Stream<Arguments> safeSingletons() {
        return Stream.of(
                Arguments.of("synchronized getInstance", (Callable<Object>) SafeSingleton::getInstance),
                Arguments.of("double-checked locking", (Callable<Object>) DoubleCheckedLockingSingleton::getInstance),
                Arguments.of("holder idiom", (Callable<Object>) HolderSingleton::getInstance));
    }

    @ParameterizedTest(name = "{0} hands every thread the same instance")
    @MethodSource("safeSingletons")
    void safeSingletonsAreUnique(String label, Callable<Object> getInstance)
            throws InterruptedException, ExecutionException {
        List<Object> instances = Concurrently.collect(THREADS, getInstance);

        Object first = instances.get(0);
        assertNotNull(first, label + " returned null");
        for (Object instance : instances) {
            assertSame(first, instance, label + " produced more than one instance");
        }
        assertEquals(1, instances.stream().distinct().count());
    }

    /**
     * The unsafe variant is not asserted to fail: the race is permitted, not required, and on a
     * given run it may well return a single instance. What is guaranteed is that whatever it hands
     * back is a usable, non-null instance -- so this documents the hazard without a flaky assertion.
     */
    @Test
    void unsafeSingletonStillReturnsUsableInstances() throws InterruptedException, ExecutionException {
        List<UnsafeSingleton> instances = Concurrently.collect(THREADS, UnsafeSingleton::getInstance);

        instances.forEach(instance -> assertNotNull(instance, "getInstance() returned null"));
    }
}
