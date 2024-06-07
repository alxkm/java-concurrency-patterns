package org.alxkm.patterns.balking;

/**
 * The BalkingPatternExample class demonstrates the Balking design pattern, which prevents an action from
 * being executed if it is not in the appropriate state. It provides methods for changing and saving state,
 * ensuring that the save operation is only performed when the state is dirty.
 */
public class BalkingPatternExample {
    private boolean isDirty = false;

    /**
     * Marks the state as dirty, indicating that it needs to be saved.
     */
    public synchronized void change() {
        isDirty = true;
    }

    /**
     * Saves the state if it is dirty, preventing unnecessary saving operations.
     */
    public synchronized void save() {
        if (!isDirty) {
            return; // Balking: If not dirty, do nothing
        }
        // perform saving
        isDirty = false;
    }
}

