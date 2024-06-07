package org.alxkm.patterns.singleton;

/**
 * Ensures that a class has only one instance and provides a global point of access to it.
 * The Singleton class demonstrates the Singleton design pattern with lazy initialization and thread safety.
 */
public class Singleton {
    // The singleton instance, declared as volatile to ensure visibility of changes to variables across threads
    private static volatile Singleton instance;

    // Private constructor to prevent instantiation
    private Singleton() {
    }

    /**
     * Returns the singleton instance of the Singleton class.
     * This method uses double-checked locking to ensure that only one instance of the class is created,
     * and it is thread-safe.
     *
     * @return The singleton instance.
     */
    public static Singleton getInstance() {
        if (instance == null) { // First check (no locking)
            synchronized (Singleton.class) { // Lock on the class object
                if (instance == null) { // Second check (with locking)
                    instance = new Singleton(); // Create the singleton instance
                }
            }
        }
        return instance; // Return the singleton instance
    }
}
