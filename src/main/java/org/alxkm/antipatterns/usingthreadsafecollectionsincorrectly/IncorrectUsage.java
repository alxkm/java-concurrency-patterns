package org.alxkm.antipatterns.usingthreadsafecollectionsincorrectly;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe collections in Java, such as those from the java.util.concurrent package,
 * are designed to handle concurrent access.
 * However, improper use of these collections can still lead to concurrency issues.
 * <p>
 * <p>
 * In this example, even though CopyOnWriteArrayList is thread-safe,
 * the addIfAbsent method is not thread-safe because the contains check and the add operation are not atomic.
 * This can lead to a race condition where multiple threads might add the same element simultaneously.
 */
public class IncorrectUsage implements BaseListUsage<String> {
    private final List<String> list = new CopyOnWriteArrayList<>();

    /**
     * Adds a new element to the list if it's not already present.
     * This method is not thread-safe despite using a thread-safe collection.
     *
     * @param element the element to add to the list.
     */
    public void addIfAbsent(String element) {
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

