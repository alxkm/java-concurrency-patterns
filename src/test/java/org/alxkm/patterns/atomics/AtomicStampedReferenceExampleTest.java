package org.alxkm.patterns.atomics;

import org.alxkm.patterns.atomics.AtomicStampedReferenceExample.Node;
import org.alxkm.patterns.atomics.AtomicStampedReferenceExample.PlainStack;
import org.alxkm.patterns.atomics.AtomicStampedReferenceExample.Stack;
import org.alxkm.patterns.atomics.AtomicStampedReferenceExample.StampedStack;
import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the ABA example. The ABA interleaving is forced, not hoped for: the other thread's work
 * runs at the exact point between the pop's read and its CAS.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class AtomicStampedReferenceExampleTest {

    /**
     * The failure itself: the plain CAS succeeds and puts B, which another thread popped, back on the
     * stack. If this ever passes as [C], the example no longer shows the problem.
     */
    @Test
    void aPlainReferenceFallsForAba() {
        assertEquals(List.of("B", "C"), AtomicStampedReferenceExample.abaScenario(new PlainStack()));
    }

    @Test
    void theStampCatchesAba() {
        assertEquals(List.of("C"), AtomicStampedReferenceExample.abaScenario(new StampedStack()));
    }

    @Test
    void popReturnsWhatWasPushedLastInFirstOut() {
        Stack stack = new StampedStack();
        Node first = new Node("first");
        Node second = new Node("second");
        stack.push(first);
        stack.push(second);

        assertSame(second, stack.pop());
        assertSame(first, stack.pop());
        assertNull(stack.pop());
    }

    /**
     * Real threads, fresh nodes: no node can come back, so no ABA, and both stacks must lose nothing and
     * hand out nothing twice. This is the check that the stamped version is still a working stack.
     */
    @Test
    void underContentionEveryPushedNodeIsPoppedExactlyOnce() throws InterruptedException {
        for (Stack stack : List.of(new PlainStack(), new StampedStack())) {
            AtomicInteger ids = new AtomicInteger();
            Set<String> popped = ConcurrentHashMap.newKeySet();
            AtomicInteger duplicates = new AtomicInteger();

            Concurrently.run(8, 5_000, () -> {
                stack.push(new Node(String.valueOf(ids.incrementAndGet())));
                Node node = stack.pop();
                if (node != null && !popped.add(node.value())) {
                    duplicates.incrementAndGet();
                }
            });

            Set<String> all = new HashSet<>(popped);
            all.addAll(stack.values());
            String name = stack.getClass().getSimpleName();
            assertEquals(0, duplicates.get(), name + ": a node was popped twice");
            assertEquals(40_000, all.size(), name + ": every pushed node is popped or still on the stack");
            assertTrue(stack.values().isEmpty(), name + ": each thread popped as many as it pushed");
        }
    }

    @Test
    void mainRuns() {
        AtomicStampedReferenceExample.main(new String[0]);
    }
}
