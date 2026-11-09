package org.alxkm.patterns.executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for CompletionServiceExample.
 * <p>
 * The point of a CompletionService is the order results come out in, so that is what these tests pin
 * down. They do it without timing: the slow task cannot finish until the fast one has been handled, so
 * the order is forced rather than likely.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class CompletionServiceExampleTest {

    /**
     * The slow task is submitted first and waits until the fast result has been handled. Calling get()
     * on the futures in submission order would wait on the slow task until it gave up. The completion
     * service hands over the fast result first, and handling it is what lets the slow task finish.
     */
    @Test
    void resultsArriveInCompletionOrderNotSubmissionOrder() throws Exception {
        CountDownLatch fastHandled = new CountDownLatch(1);
        List<String> handled = new ArrayList<>();

        List<Callable<String>> tasks = List.of(
                () -> {
                    if (!fastHandled.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("the fast result was never handled");
                    }
                    return "slow";
                },
                () -> "fast");

        CompletionServiceExample.executeTasks(tasks, result -> {
            handled.add(result);
            if ("fast".equals(result)) {
                fastHandled.countDown();
            }
        });

        assertEquals(List.of("fast", "slow"), handled);
    }

    @Test
    void everyResultIsHandledExactlyOnce() throws Exception {
        List<Callable<String>> tasks = IntStream.range(0, 50)
                .mapToObj(i -> (Callable<String>) () -> "task " + i)
                .toList();
        List<String> handled = new ArrayList<>();

        CompletionServiceExample.executeTasks(tasks, handled::add);

        Set<String> expected = IntStream.range(0, 50).mapToObj(i -> "task " + i).collect(Collectors.toSet());
        assertEquals(50, handled.size(), "each result should be handled once");
        assertEquals(expected, Set.copyOf(handled));
    }

    @Test
    void aFailingTaskReachesTheCallerWithItsCause() {
        List<Callable<String>> tasks = List.of(() -> {
            throw new IllegalStateException("boom");
        });

        ExecutionException thrown = assertThrows(ExecutionException.class,
                () -> CompletionServiceExample.executeTasks(tasks, result -> { }));

        assertEquals("boom", thrown.getCause().getMessage());
    }
}
