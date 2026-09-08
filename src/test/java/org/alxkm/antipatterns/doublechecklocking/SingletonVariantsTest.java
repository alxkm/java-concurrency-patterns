package org.alxkm.antipatterns.doublechecklocking;

import org.alxkm.testsupport.Concurrently;
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
 * Covers the two correct members of this package: double-checked locking with a volatile field, and
 * the initialization-on-demand holder. The non-volatile {@link Singleton} is the counter-example and
 * is deliberately not asserted to be safe.
 */
class SingletonVariantsTest {
    private static final int THREADS = 64;

    private static Stream<Arguments> correctSingletons() {
        return Stream.of(
                Arguments.of("volatile double-checked locking",
                        (Callable<Object>) SingletonWithVolatile::getInstance),
                Arguments.of("initialization-on-demand holder",
                        (Callable<Object>) SingletonInitializationOnDemand::getInstance));
    }

    @ParameterizedTest(name = "{0} hands every thread the same instance")
    @MethodSource("correctSingletons")
    void correctSingletonsAreUnique(String label, Callable<Object> getInstance)
            throws InterruptedException, ExecutionException {
        List<Object> instances = Concurrently.collect(THREADS, getInstance);

        Object first = instances.get(0);
        assertNotNull(first, label + " returned null");
        for (Object instance : instances) {
            assertSame(first, instance, label + " produced more than one instance");
        }
        assertEquals(1, instances.stream().distinct().count());
    }
}
