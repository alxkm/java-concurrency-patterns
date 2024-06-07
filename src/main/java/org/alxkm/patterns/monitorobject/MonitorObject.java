package org.alxkm.patterns.monitorobject;

/**
 * The MonitorObject class encapsulates a lock in an object
 * to provide thread-safe methods for accessing shared resources.
 */
public class MonitorObject {
    private final Object lock = new Object();
    private int count = 0;

    /**
     * Increments the count in a thread-safe manner.
     */
    public void increment() {
        synchronized (lock) {
            count++;
        }
    }

    /**
     * Retrieves the count in a thread-safe manner.
     *
     * @return the current count
     */
    public int getCount() {
        synchronized (lock) {
            return count;
        }
    }
}
