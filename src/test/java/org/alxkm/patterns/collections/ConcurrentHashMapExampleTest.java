package org.alxkm.patterns.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrentHashMapExampleTest {

    /**
     * This test method verifies the behavior of the ConcurrentHashMapExample class, which demonstrates
     * the usage of the ConcurrentHashMap class for concurrent access. It creates 10 threads, each of
     * which adds a key-value pair to the ConcurrentHashMap instance concurrently. After all threads have
     * added their key-value pairs, the test checks that each key added by the threads is present in the map
     * and that the corresponding value matches the expected value. This test ensures that the ConcurrentHashMap
     * provides safe and consistent access for concurrent operations without the need for explicit synchronization.
     */
    @Test
    public void testConcurrentHashMap() throws InterruptedException {
        ConcurrentHashMapExample example = new ConcurrentHashMapExample();
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                example.put("key" + index, index);
                assertTrue(example.containsKey("key" + index));
                assertEquals(index, example.get("key" + index));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
