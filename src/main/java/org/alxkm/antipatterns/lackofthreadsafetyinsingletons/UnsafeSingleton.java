package org.alxkm.antipatterns.lackofthreadsafetyinsingletons;

/**
 *
 * A common issue with singletons is the lack of proper synchronization,
 * which can lead to multiple instances being created in a multithreaded environment.
 * <p>
 * In this example, the getInstance method is not synchronized,
 * which can lead to multiple instances being created if multiple threads access the method simultaneously.
 *
 */
public class UnsafeSingleton {
    private static UnsafeSingleton instance;

    /**
     * Private constructor to prevent instantiation.
     */
    private UnsafeSingleton() {
    }

    /**
     * Returns the singleton instance. This method is not synchronized,
     * leading to potential creation of multiple instances in a multithreaded environment.
     *
     * @return the singleton instance.
     */
    public static UnsafeSingleton getInstance() {
        if (instance == null) {
            instance = new UnsafeSingleton();
        }
        return instance;
    }

    public void showMessage() {
        System.out.println("UnsafeSingleton instance method called.");
    }

    public static void main(String[] args) {
        Runnable task = () -> {
            UnsafeSingleton singleton = UnsafeSingleton.getInstance();
            singleton.showMessage();
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();
    }
}
