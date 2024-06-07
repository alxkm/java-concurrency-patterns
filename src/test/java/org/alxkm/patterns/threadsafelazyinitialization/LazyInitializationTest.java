package org.alxkm.patterns.threadsafelazyinitialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

public class LazyInitializationTest {

    /**
     * This test method verifies the lazy initialization of a singleton instance
     * using the Lazy Initialization pattern. It ensures that multiple calls to
     * the getInstance method of the LazyInitialization class return the same instance.
     */
    @Test
    public void testLazyInitialization() {
        LazyInitialization instance1 = LazyInitialization.getInstance();
        LazyInitialization instance2 = LazyInitialization.getInstance();

        assertSame(instance1, instance2);
    }
}
