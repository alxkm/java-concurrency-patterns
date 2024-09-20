package org.alxkm.antipatterns.usingthreadsafecollectionsincorrectly;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * To resolve this issue, we need to ensure that the check and add operation are performed atomically.
 * One way to do this is to synchronize the method or the critical section.
 *
 * By synchronizing the addIfAbsent method, we ensure that only one thread can execute it at a time, making the operation atomic.
 *
 */
public class CorrectUsage implements BaseListUsage<String> {
    private final List<String> list = new CopyOnWriteArrayList<>();

    /**
     * Adds a new element to the list if it's not already present.
     * This method is synchronized to ensure thread-safety.
     *
     * @param element the element to add to the list.
     */
    public synchronized void addIfAbsent(String element) {
        if (!list.contains(element)) {
            list.add(element);
        }
    }

    /**
     * Returns the size of the list.
     *
     * @return the number of elements in the list.
     */
    public int size() {
        return list.size();
    }

    @Override
    public List<String> getCollection() {
        return list;
    }
}
