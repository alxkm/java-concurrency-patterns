package org.alxkm.antipatterns.lackofthreadsafetyinsingletons;

/**
 *
 * Alternative Resolution: Initialization-on-Demand Holder Idiom
 * Another approach to implement a thread-safe singleton is the Initialization-on-Demand Holder idiom, which leverages the class loader mechanism to ensure thread safety.
 * <p>
 * In this version, the Holder class is loaded on the first invocation of getInstance(), ensuring thread safety through the class loader mechanism.
 *
 **/
public class HolderSingleton {
    /**
     * Private constructor to prevent instantiation.
     */
    private HolderSingleton() {
    }

    /**
     * Static inner class - inner classes are not loaded until they are referenced.
     */
    private static class Holder {
        private static final HolderSingleton INSTANCE = new HolderSingleton();
    }

    /**
     * Returns the singleton instance.
     * This implementation uses the Initialization-on-Demand Holder idiom to ensure thread safety.
     *
     * @return the singleton instance.
     */
    public static HolderSingleton getInstance() {
        return Holder.INSTANCE;
    }

    public void showMessage() {
        System.out.println("HolderSingleton instance method called.");
    }

    public static void main(String[] args) {
        Runnable task = () -> {
            HolderSingleton singleton = HolderSingleton.getInstance();
            singleton.showMessage();
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();
    }
}
