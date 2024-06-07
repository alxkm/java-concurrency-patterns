package org.alxkm.antipatterns.doublechecklocking;

/**
 * Starting from Java 5, the volatile keyword ensures that changes to a variable are visible to all threads,
 * which fixes the double-checked locking pattern.
 * <p>
 * <p>
 * In this revised example, the volatile keyword is used for the instance variable,
 * ensuring proper visibility of changes across threads and fixing the broken double-checked locking pattern.
 */
public class SingletonWithVolatile {
    private static volatile SingletonWithVolatile instance;

    /**
     * Private constructor to prevent instantiation.
     */
    private SingletonWithVolatile() {
    }

    /**
     * Returns the singleton instance.
     * This implementation uses volatile keyword to ensure thread safety.
     *
     * @return the singleton instance.
     */
    public static SingletonWithVolatile getInstance() {
        if (instance == null) { // First check (not synchronized)
            synchronized (Singleton.class) {
                if (instance == null) { // Second check (synchronized)
                    instance = new SingletonWithVolatile();
                }
            }
        }
        return instance;
    }

    public void showMessage() {
        System.out.println("Singleton instance method called.");
    }

    public static void main(String[] args) {
        SingletonWithVolatile singleton = SingletonWithVolatile.getInstance();
        singleton.showMessage();
    }
}
