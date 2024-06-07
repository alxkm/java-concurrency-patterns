package org.alxkm.patterns.collections;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The CopyOnWriteArrayListExample class demonstrates the usage of CopyOnWriteArrayList, which is a thread-safe variant of ArrayList.
 * It provides methods to add, check for the presence of, and remove elements from the list.
 */
public class CopyOnWriteArrayListExample {
    private final CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

    /**
     * Adds the specified element to the list.
     *
     * @param element The element to be added to the list.
     */
    public void add(String element) {
        list.add(element);
    }

    /**
     * Checks whether the list contains the specified element.
     *
     * @param element The element whose presence in the list is to be tested.
     * @return true if the list contains the specified element, otherwise false.
     */
    public boolean contains(String element) {
        return list.contains(element);
    }

    /**
     * Removes the specified element from the list if it is present.
     *
     * @param element The element to be removed from the list, if present.
     */
    public void remove(String element) {
        list.remove(element);
    }
}
