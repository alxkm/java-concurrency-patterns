package org.alxkm.antipatterns.lackofthreadsafetyinsingletons;

/**
 *
 * To ensure thread safety, we can synchronize the getInstance method.
 * <p>
 * In this revised example, the getInstance method is synchronized,
 * ensuring that only one instance is created even when multiple threads access the method simultaneously.
 *
 */
public class SafeSingleton {
    private static SafeSingleton instance;

    /**
     * Private constructor to prevent instantiation.
     */
    private SafeSingleton() {
    }

    /**
     * Returns the singleton instance. This method is synchronized to ensure
     * that only one instance is created in a multithreaded environment.
     *
     * @return the singleton instance.
     */
    public static synchronized SafeSingleton getInstance() {
        if (instance == null) {
            instance = new SafeSingleton();
        }
        return instance;
    }

    public void showMessage() {
        System.out.println("SafeSingleton instance method called.");
    }

    public static void main(String[] args) {
        Runnable task = () -> {
            SafeSingleton singleton = SafeSingleton.getInstance();
            singleton.showMessage();
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();
    }
}

