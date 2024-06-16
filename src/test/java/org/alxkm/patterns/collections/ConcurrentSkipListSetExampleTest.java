package org.alxkm.patterns.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrentSkipListSetExampleTest {

    /**
     * This test method verifies the behavior of the ConcurrentSkipListSetExample class, which demonstrates
     * the usage of the ConcurrentSkipListSet class for concurrent access. It creates 10 threads, each of
     * which adds an element to the ConcurrentSkipListSet instance concurrently. After all threads have added
     * their elements, the test checks that each element added by the threads is present in the set. Then,
     * the test removes each element added by the threads and verifies that the set does not contain any of
     * these elements. This test ensures that the ConcurrentSkipListSet provides safe and consistent access
     * for concurrent operations without the need for explicit synchronization.
     */
    @Test
    public void testConcurrentSkipListSet() throws InterruptedException {
        ConcurrentSkipListSetExample example = new ConcurrentSkipListSetExample();
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                example.add(index);
                assertTrue(example.contains(index));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (int i = 0; i < 10; i++) {
            example.remove(i);
            assertFalse(example.contains(i));
        }
    }
}

