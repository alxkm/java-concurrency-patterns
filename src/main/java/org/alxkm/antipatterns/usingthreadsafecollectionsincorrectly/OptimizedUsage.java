package org.alxkm.antipatterns.usingthreadsafecollectionsincorrectly;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Another approach is to use a ConcurrentHashMap to achieve atomic check-and-add operations.
 * The ConcurrentHashMap provides thread-safe methods, simplifying the code.
 *
 * In this case, ConcurrentHashMap.newKeySet() creates a thread-safe set backed by a ConcurrentHashMap.
 * The add method of this set is atomic, ensuring that the element is added only if it is not already present.
 */
public class OptimizedUsage implements BaseListUsage<String> {
    private final Collection<String> set = ConcurrentHashMap.newKeySet();

    /**
     * Adds a new element to the set if it's not already present.
     * This method uses ConcurrentHashMap to ensure thread-safety.
     *
     * @param element the element to add to the set.
     */
    public void addIfAbsent(String element) {
        set.add(element);
    }

    /**
     * Returns the size of the set.
     *
     * @return the number of elements in the set.
     */
    public int size() {
        return set.size();
    }

    @Override
    public Collection<String> getCollection() {
        return set;
    }
}
