package org.alxkm.patterns.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CopyOnWriteArrayListExampleTest {

    /**
     * This test method verifies the behavior of the CopyOnWriteArrayListExample class, which demonstrates
     * the usage of the CopyOnWriteArrayList class for concurrent access. It creates 10 threads, each of
     * which adds an element to the CopyOnWriteArrayList instance concurrently. After all threads have added
     * their elements, the test checks that each element added by the threads is present in the list. Then,
     * the test removes each element added by the threads and verifies that the list does not contain any of
     * these elements. This test ensures that the CopyOnWriteArrayList provides safe and consistent access
     * for concurrent operations without the need for explicit synchronization.
     */
    @Test
    public void testCopyOnWriteArrayList() throws InterruptedException {
        CopyOnWriteArrayListExample example = new CopyOnWriteArrayListExample();
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                example.add("element" + index);
                assertTrue(example.contains("element" + index));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (int i = 0; i < 10; i++) {
            example.remove("element" + i);
            assertFalse(example.contains("element" + i));
        }
    }
}
