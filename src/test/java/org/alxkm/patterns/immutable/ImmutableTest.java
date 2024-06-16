package org.alxkm.patterns.immutable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImmutableTest {

    /**
     * This test method verifies the immutability of the Immutable class, which is designed to
     * encapsulate an immutable value. It creates an instance of the Immutable class with a value
     * of 42 and then asserts that the getValue method returns the expected value, ensuring that
     * the value set during object construction remains unchanged and immutable.
     */
    @Test
    public void testImmutable() {
        Immutable immutable = new Immutable(42);

        assertEquals(42, immutable.getValue());
    }
}
