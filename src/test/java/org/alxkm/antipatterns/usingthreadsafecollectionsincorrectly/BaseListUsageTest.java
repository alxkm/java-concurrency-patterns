package org.alxkm.antipatterns.usingthreadsafecollectionsincorrectly;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseListUsageTest {
    private static final int THREADS = 8;
    private static final int RANGE = 500;

    private static Stream<Arguments> atomicImplementations() {
        return Stream.of(
                Arguments.of("CorrectUsage", (Supplier<BaseListUsage<String>>) CorrectUsage::new),
                Arguments.of("OptimizedUsage", (Supplier<BaseListUsage<String>>) OptimizedUsage::new));
    }

    /**
     * An atomic check-then-add must end up with exactly one entry per distinct element, however many
     * threads offered it concurrently.
     */
    @ParameterizedTest(name = "{0} adds each element exactly once")
    @MethodSource("atomicImplementations")
    void atomicImplementationsAddEachElementOnce(String label,
                                                 Supplier<BaseListUsage<String>> factory) throws InterruptedException {
        BaseListUsage<String> usage = factory.get();

        addRangeConcurrently(usage);

        assertEquals(RANGE, usage.size(), label + " added duplicates");
        assertEquals(RANGE, new HashSet<>(usage.getCollection()).size());
    }

    /**
     * The non-atomic variant can only ever add an element more than once, never drop one, since the
     * add itself is thread-safe and only the surrounding check is racy. Asserting the invariant
     * keeps the test deterministic; the demonstration that the race does surface in practice lives
     * in {@link UsageExample}.
     */
    @Test
    void incorrectUsageNeverLosesElements() throws InterruptedException {
        BaseListUsage<String> usage = new IncorrectUsage();

        addRangeConcurrently(usage);

        Set<String> distinct = new HashSet<>(usage.getCollection());
        assertEquals(RANGE, distinct.size(), "every element should be present at least once");
        assertTrue(usage.size() >= RANGE, "a thread-safe add cannot lose elements");
    }

    private static void addRangeConcurrently(BaseListUsage<String> usage) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        try {
            for (int t = 0; t < THREADS; t++) {
                executor.execute(() -> {
                    try {
                        startGate.await();
                        for (int i = 0; i < RANGE; i++) {
                            usage.addIfAbsent("Element " + i);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }

            startGate.countDown();
            assertTrue(finished.await(30, TimeUnit.SECONDS), "workers did not finish in time");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "executor did not terminate");
        }
    }
}
