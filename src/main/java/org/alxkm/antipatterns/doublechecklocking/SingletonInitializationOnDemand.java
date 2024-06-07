package org.alxkm.antipatterns.doublechecklocking;

/**
 * Another approach to implement lazy initialization in a thread-safe manner is to use the Initialization-on-Demand Holder idiom,
 * which leverages the class loader mechanism to ensure thread safety.
 * <p>
 * <p>
 * In this version, the Holder class is loaded on the first invocation of getInstance(),
 * and since class loading is thread-safe, this approach ensures that the singleton instance is created in a thread-safe manner without the need for synchronization or volatile variables.
 */
public class SingletonInitializationOnDemand {
    /**
     * Private constructor to prevent instantiation.
     */
    private SingletonInitializationOnDemand() {
    }

    /**
     * Static inner class - inner classes are not loaded until they are referenced.
     */
    private static class Holder {
        private static final SingletonInitializationOnDemand INSTANCE = new SingletonInitializationOnDemand();
    }

    /**
     * Returns the singleton instance.
     * This implementation uses the Initialization-on-Demand Holder idiom to ensure thread safety.
     *
     * @return the singleton instance.
     */
    public static SingletonInitializationOnDemand getInstance() {
        return Holder.INSTANCE;
    }

    public void showMessage() {
        System.out.println("Singleton instance method called.");
    }

    public static void main(String[] args) {
        SingletonInitializationOnDemand singleton = SingletonInitializationOnDemand.getInstance();
        singleton.showMessage();
    }
}
