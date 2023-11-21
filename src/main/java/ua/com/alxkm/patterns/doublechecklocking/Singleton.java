package ua.com.alxkm.patterns.doublechecklocking;

/**
 * Singleton class implementing a thread-safe Singleton pattern.
 */
public class Singleton {
    private volatile static Singleton instance;

    /**
     * Private constructor to prevent instantiation.
     */
    private Singleton() {
        // Private constructor to prevent instantiation.
    }

    /**
     * Returns the instance of the Singleton class.
     *
     * @return The Singleton instance.
     */
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}