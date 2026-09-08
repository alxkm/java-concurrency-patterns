package org.alxkm.antipatterns.racecondition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountAmountTest {
    private static final int THREADS = 4;
    private static final int INCREMENTS_PER_THREAD = 50_000;
    private static final int EXPECTED = THREADS * INCREMENTS_PER_THREAD;

    private static Stream<Arguments> safeStrategies() {
        return Stream.of(
                Arguments.of("synchronized",
                        (Consumer<AccountAmount>) AccountAmount::safeSynchronizedIncrementAmount,
                        (ToIntFunction<AccountAmount>) AccountAmount::getSynchronizedAmount),
                Arguments.of("ReentrantLock",
                        (Consumer<AccountAmount>) AccountAmount::safeLockIncrementAmount,
                        (ToIntFunction<AccountAmount>) AccountAmount::getLockAmount),
                Arguments.of("AtomicInteger",
                        (Consumer<AccountAmount>) AccountAmount::incrementAtomicAmount,
                        (ToIntFunction<AccountAmount>) AccountAmount::getAtomicAmount));
    }

    /**
     * Every correctly guarded strategy must account for all increments under contention.
     */
    @ParameterizedTest(name = "{0} loses no increments")
    @MethodSource("safeStrategies")
    void safeStrategiesLoseNoIncrements(String label,
                                        Consumer<AccountAmount> increment,
                                        ToIntFunction<AccountAmount> read) throws InterruptedException {
        assertEquals(EXPECTED, incrementConcurrently(increment, read), label + " lost increments");
    }

    /**
     * The unsynchronized increment can only ever lose updates, never invent them. A lost update is
     * a race the JVM is permitted but not obliged to expose, so this asserts the invariant that
     * always holds rather than demanding that the race manifest on this particular run.
     */
    @Test
    void unsafeStrategyNeverExceedsTheExpectedTotal() throws InterruptedException {
        int actual = incrementConcurrently(
                AccountAmount::unsafeIncrementAmount, AccountAmount::getUnsafeAmount);

        assertTrue(actual <= EXPECTED, "observed " + actual + " increments, more than the " + EXPECTED + " performed");
        assertTrue(actual > 0, "expected at least some increments to land");
    }

    private static int incrementConcurrently(Consumer<AccountAmount> increment,
                                             ToIntFunction<AccountAmount> read) throws InterruptedException {
        AccountAmount accountAmount = new AccountAmount();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        try {
            for (int i = 0; i < THREADS; i++) {
                executor.execute(() -> {
                    try {
                        startGate.await();
                        for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                            increment.accept(accountAmount);
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

        return read.applyAsInt(accountAmount);
    }
}
