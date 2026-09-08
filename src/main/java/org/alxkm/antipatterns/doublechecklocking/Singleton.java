package org.alxkm.antipatterns.doublechecklocking;


/**
 * Double-checked locking reduces the cost of acquiring a lock by testing the locking criterion once
 * without the lock, and again while holding it.
 * <p>
 * This particular implementation is broken, and not only on old JVMs. Because {@code instance} is
 * not {@code volatile}, the unsynchronized first read carries no ordering guarantee: a thread can
 * observe a non-null reference while the constructor's writes to the new object are still invisible
 * to it, handing the caller a partially constructed instance. That hazard is present on every Java
 * version, today included.
 * <p>
 * What changed in Java 5 is the fix, not the bug. The revised Java Memory Model gave {@code
 * volatile} the acquire/release semantics that make the pattern sound, so before Java 5 there was no
 * way to write double-checked locking correctly at all -- adding {@code volatile} would not have
 * saved it either.
 *
 * @see SingletonWithVolatile for the version that is correct on Java 5 and later.
 * @see SingletonInitializationOnDemand for the holder idiom, which needs no locking at all.
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
            synchronized (Singleton.class) {
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
