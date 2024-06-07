package org.alxkm.antipatterns.doublechecklocking;


/**
 * Double-checked locking is a design pattern used to reduce the overhead of acquiring a lock
 * by first testing the locking criterion without actually acquiring the lock.
 * However, the original implementation of this pattern was broken in Java before version 5 due to the lack of guaranteed visibility of changes to variables across threads.
 * <p>
 * <p>
 * In this example, the double-checked locking pattern is used to lazily initialize the singleton instance.
 * However, this implementation is broken in Java versions before Java 5 due to issues with the Java Memory Model (JMM),
 * which could cause the instance variable to appear initialized before it is fully constructed.
 */
public class Singleton {
    private static Singleton instance;

    /**
     * Private constructor to prevent instantiation.
     */
    private Singleton() {
    }

    /**
     * Returns the singleton instance.
     * This implementation of double-checked locking is broken in pre-Java 5 versions.
     *
     * @return the singleton instance.
     */
    public static Singleton getInstance() {
        if (instance == null) { // First check (not synchronized)
            synchronized ( Singleton.class) {
                if (instance == null) { // Second check (synchronized)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }

    public void showMessage() {
        System.out.println("Singleton instance method called.");
    }

    public static void main(String[] args) {
        Singleton singleton = Singleton.getInstance();
        singleton.showMessage();
    }
}
