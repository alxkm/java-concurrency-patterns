package org.alxkm.patterns.threadsafelazyinitialization;

/**
 * Ensures a class's instance is lazily initialized in a thread-safe manner.
 * The LazyInitialization class demonstrates the Singleton design pattern using the Bill Pugh Singleton Design.
 */
public class LazyInitialization {
    // Private static inner class that contains the instance of the Singleton class.
    // When the LazyInitialization class is loaded, the Holder class is not loaded into memory and only when someone calls the getInstance method, this class gets loaded and creates the Singleton class instance.
    private static class Holder {
        private static final LazyInitialization INSTANCE = new LazyInitialization(); // The singleton instance
    }

    // Private constructor to prevent instantiation
    private LazyInitialization() {
    }

    /**
     * Returns the singleton instance of the LazyInitialization class.
     * This method uses the Bill Pugh Singleton Design, which leverages the Java language's guarantees about class initialization to ensure thread safety.
     *
     * @return The singleton instance.
     */
    public static LazyInitialization getInstance() {
        return Holder.INSTANCE; // Return the singleton instance
    }
}
