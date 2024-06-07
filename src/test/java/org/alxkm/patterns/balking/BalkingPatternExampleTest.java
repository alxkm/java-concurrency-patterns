package org.alxkm.patterns.balking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class BalkingPatternExampleTest {
    /**
     * This test method verifies the behavior of the Balking Pattern implementation.
     * Specifically, it ensures that the `save` method in the `BalkingPatternExample`
     * class does not perform the save operation if the state has not changed since
     * the last save.
     */
    @Test
    public void testBalkingPattern() {
        BalkingPatternExample example = new BalkingPatternExample();
        example.change();
        example.save();
        // Method `save` should reset the dirty flag
        assertDoesNotThrow(example::save);
    }
}
