package org.alxkm.patterns.singleton;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

public class DoubleCheckedLockingSingletonTest {

    /**
     * This test method verifies that the Singleton pattern is correctly implemented
     * by ensuring that multiple calls to the getInstance method of the Singleton class
     * return the same instance.
     */
    @Test
    public void testSingletonInstance() {
        Singleton instance1 = Singleton.getInstance();
        Singleton instance2 = Singleton.getInstance();

        assertSame(instance1, instance2);
    }
}
