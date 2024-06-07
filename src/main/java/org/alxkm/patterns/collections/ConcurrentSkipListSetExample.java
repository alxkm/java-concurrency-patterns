package org.alxkm.patterns.collections;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The ConcurrentSkipListSetExample class demonstrates the usage of ConcurrentSkipListSet, which is a thread-safe variant of TreeSet.
 * It provides methods to add, check for the presence of, and remove elements from the set.
 */
public class ConcurrentSkipListSetExample {
    private final ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<>();

    /**
     * Adds the specified element to the set.
     *
     * @param value The element to be added to the set.
     */
    public void add(int value) {
        set.add(value);
    }

    /**
     * Checks whether the set contains the specified element.
     *
     * @param value The element whose presence in the set is to be tested.
     * @return true if the set contains the specified element, otherwise false.
     */
    public boolean contains(int value) {
        return set.contains(value);
    }

    /**
     * Removes the specified element from the set if it is present.
     *
     * @param value The element to be removed from the set, if present.
     */
    public void remove(int value) {
        set.remove(value);
    }
}
