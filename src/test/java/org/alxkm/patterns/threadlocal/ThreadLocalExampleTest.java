package org.alxkm.patterns.threadlocal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadLocalExampleTest {

    /**
     * This test method verifies the usage of ThreadLocal to maintain thread-specific
     * storage in the ThreadSpecificStorageExample class. It creates two threads, t1 and t2,
     * each of which sets a different value to the ThreadLocal variable in the example object.
     * The test asserts that each thread sees its own value when accessing the ThreadLocal
     * variable, ensuring that the values are maintained separately for each thread.
     */
    @Test
    public void testThreadLocalStorage() throws InterruptedException {
        ThreadLocalExample example = new ThreadLocalExample();

        Thread t1 = new Thread(() -> {
            example.THREAD_LOCAL.set(1);
            assertEquals(1, example.THREAD_LOCAL.get());
        });

        Thread t2 = new Thread(() -> {
            example.THREAD_LOCAL.set(2);
            assertEquals(2, example.THREAD_LOCAL.get());
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }
}

