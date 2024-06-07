package org.alxkm.patterns.doublechecklocking;

/**
 * The DoubleCheckedLockingSingleton class implements the Singleton pattern using double-checked locking.
 * It ensures that only one instance of the class is created and provides a global point of access to it.
 */
public class DoubleCheckedLockingSingleton {
    private volatile static DoubleCheckedLockingSingleton instance;

    /**
     * Private constructor to prevent instantiation from outside the class.
     */
    private DoubleCheckedLockingSingleton() {
        // Private constructor to prevent instantiation.
    }

    /**
     * Returns the instance of the DoubleCheckedLockingSingleton class.
     *
     * @return The Singleton instance.
     */
    public static DoubleCheckedLockingSingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedLockingSingleton.class) {
                if (instance == null) {
                    instance = new DoubleCheckedLockingSingleton();
                }
            }
        }
        return instance;
    }
}