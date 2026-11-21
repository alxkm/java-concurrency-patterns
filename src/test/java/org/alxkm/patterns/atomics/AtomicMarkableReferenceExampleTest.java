package org.alxkm.patterns.atomics;

import org.alxkm.patterns.atomics.AtomicMarkableReferenceExample.MarkedList;
import org.alxkm.patterns.atomics.AtomicMarkableReferenceExample.PlainList;
import org.alxkm.patterns.atomics.AtomicMarkableReferenceExample.SortedIntSet;
import org.alxkm.testsupport.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the lock-free list. The two broken interleavings are forced, not hoped for: the other
 * thread's work runs at the exact point between remove's read of the successor and its CAS.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class AtomicMarkableReferenceExampleTest {

    /**
     * The failure itself: add(25) returns true, and 25 goes out of the list with 20. If this ever
     * passes as [10, 25, 30], the example no longer shows the problem.
     */
    @Test
    void aPlainListLosesAnInsertNextToARemove() {
        assertEquals(List.of(10, 30), AtomicMarkableReferenceExample.insertDuringRemove(new PlainList()));
    }

    @Test
    void theMarkKeepsAnInsertNextToARemove() {
        assertEquals(List.of(10, 25, 30), AtomicMarkableReferenceExample.insertDuringRemove(new MarkedList()));
    }

    @Test
    void aPlainListBringsBackARemovedNeighbour() {
        assertEquals(List.of(10, 30), AtomicMarkableReferenceExample.removeNeighbourDuringRemove(new PlainList()));
    }

    @Test
    void theMarkKeepsARemovedNeighbourRemoved() {
        assertEquals(List.of(10), AtomicMarkableReferenceExample.removeNeighbourDuringRemove(new MarkedList()));
    }

    /**
     * A key removed while its remove is paused before the mark is still there; the removal itself, the
     * mark, is the moment it leaves the set.
     */
    @Test
    void aKeyIsInTheSetUntilItIsMarked() {
        SortedIntSet set = new MarkedList();
        set.add(5);
        AtomicBoolean containedDuringPause = new AtomicBoolean();

        assertTrue(set.remove(5, () -> containedDuringPause.set(set.contains(5))));
        assertTrue(containedDuringPause.get());
        assertFalse(set.contains(5));
    }

    /**
     * remove(20) is paused with 10 as the predecessor; meanwhile 15 is inserted before 20. The mark still
     * succeeds, so 20 is removed, but the unlink of 10.next fails: 20 stays linked, behind 15, with its
     * mark set. A marked node must already count as gone, and the next search unlinks it.
     */
    @Test
    void aMarkedNodeIsGoneBeforeItIsUnlinked() {
        SortedIntSet set = new MarkedList();
        set.add(10);
        set.add(20);
        set.add(30);

        assertTrue(set.remove(20, () -> set.add(15)));

        assertFalse(set.contains(20));
        assertEquals(List.of(10, 15, 30), set.values());
        assertTrue(set.add(20), "the search unlinks the marked 20, and a new 20 goes in its place");
        assertEquals(List.of(10, 15, 20, 30), set.values());
    }

    @Test
    void behavesAsASortedSet() {
        SortedIntSet set = new MarkedList();

        assertTrue(set.add(3));
        assertTrue(set.add(-7));
        assertTrue(set.add(42));
        assertFalse(set.add(3), "a key already there is not added twice");
        assertEquals(List.of(-7, 3, 42), set.values());

        assertTrue(set.contains(42));
        assertFalse(set.contains(4));
        assertTrue(set.remove(3));
        assertFalse(set.remove(3), "a key is removed once");
        assertFalse(set.contains(3));
        assertEquals(List.of(-7, 42), set.values());

        assertTrue(set.add(3), "a removed key can be added again");
        assertEquals(List.of(-7, 3, 42), set.values());
    }

    @Test
    void theHeadAndTailKeysAreRejected() {
        SortedIntSet set = new MarkedList();

        assertThrows(IllegalArgumentException.class, () -> set.add(Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> set.remove(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> set.contains(Integer.MAX_VALUE));
    }

    /**
     * Real threads on the same keys. Every thread tries to add every key, then to remove every key. For
     * each key exactly one add and exactly one remove must succeed, and the set must end up empty. The
     * threads walk the keys from different starting points, so in the second phase one thread removes a
     * key while another removes its neighbour.
     */
    @Test
    void eachKeyIsAddedOnceAndRemovedOnceUnderContention() throws Exception {
        int threads = 8;
        int keys = 400;
        for (int run = 0; run < 20; run++) {
            SortedIntSet set = new MarkedList();
            AtomicIntegerArray adds = new AtomicIntegerArray(keys);
            AtomicIntegerArray removes = new AtomicIntegerArray(keys);
            AtomicInteger nextThread = new AtomicInteger();
            // No thread starts removing until all have finished adding; otherwise a key removed early
            // could rightly be added again by a thread still in its first loop.
            CyclicBarrier addsDone = new CyclicBarrier(threads);

            Concurrently.run(threads, 1, () -> {
                int offset = nextThread.getAndIncrement() * keys / threads;
                for (int i = 0; i < keys; i++) {
                    int key = (offset + i) % keys;
                    if (set.add(key)) {
                        adds.incrementAndGet(key);
                    }
                }
                await(addsDone);
                for (int i = 0; i < keys; i++) {
                    int key = (offset + i) % keys;
                    if (set.remove(key)) {
                        removes.incrementAndGet(key);
                    }
                }
            });

            for (int key = 0; key < keys; key++) {
                assertEquals(1, adds.get(key), "successful adds of " + key + " in run " + run);
                assertEquals(1, removes.get(key), "successful removes of " + key + " in run " + run);
            }
            assertEquals(List.of(), set.values(), "run " + run);
        }
    }

    /**
     * Inserts next to removes, the case that loses keys in the plain list. The set starts with the even
     * keys. Each thread owns every eighth key: it adds its odd keys and removes its keys of the form
     * 4n + 2. So while one thread removes 2, others insert 1 and 3 on either side of it. What is left must
     * be exactly the keys that were not removed.
     */
    @Test
    void insertsNextToRemovesAreNotLost() throws Exception {
        int threads = 8;
        int keys = 2_000;
        for (int run = 0; run < 20; run++) {
            SortedIntSet set = new MarkedList();
            IntStream.range(0, keys).filter(k -> k % 2 == 0).forEach(set::add);
            AtomicInteger nextThread = new AtomicInteger();

            Concurrently.run(threads, 1, () -> {
                int owner = nextThread.getAndIncrement();
                for (int key = owner; key < keys; key += threads) {
                    if (key % 2 == 1) {
                        set.add(key);
                    } else if (key % 4 == 2) {
                        set.remove(key);
                    }
                }
            });

            List<Integer> expected = new ArrayList<>();
            IntStream.range(0, keys).filter(k -> k % 4 != 2).forEach(expected::add);
            assertEquals(expected, set.values(), "run " + run);
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (BrokenBarrierException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void mainRuns() {
        AtomicMarkableReferenceExample.main(new String[0]);
    }
}
