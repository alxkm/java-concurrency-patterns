package org.alxkm.antipatterns.lackofthreadsafetyinsingletons;

/**
 *
 * Another approach is to use double-checked locking with the volatile keyword to minimize synchronization overhead.
 * <p>
 * In this version, the volatile keyword ensures visibility of changes to the instance variable across threads,
 * while double-checked locking minimizes synchronization overhead.
 *
 */
public class DoubleCheckedLockingSingleton {
    private static volatile DoubleCheckedLockingSingleton instance;

    /**
     * Private constructor to prevent instantiation.
     */
    private DoubleCheckedLockingSingleton() {
    }

    /**
     * Returns the singleton instance using double-checked locking with volatile.
     *
     * @return the singleton instance.
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

    public void showMessage() {
        System.out.println("DoubleCheckedLockingSingleton instance method called.");
    }

    public static void main(String[] args) {
        Runnable task = () -> {
            DoubleCheckedLockingSingleton singleton = DoubleCheckedLockingSingleton.getInstance();
            singleton.showMessage();
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();
    }
}
