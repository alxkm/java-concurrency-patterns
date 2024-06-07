package org.alxkm.patterns.collections;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The ConcurrentHashMapExample class demonstrates the usage of the ConcurrentHashMap, which is a thread-safe variant of HashMap.
 * It provides methods to put, get, and check for the presence of a key in the concurrent hash map.
 */
public class ConcurrentHashMapExample {
    private final ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

    /**
     * Puts a key-value pair into the concurrent hash map.
     *
     * @param key   The key to be inserted into the map.
     * @param value The value corresponding to the key to be inserted into the map.
     */
    public void put(String key, Integer value) {
        map.put(key, value);
    }

    /**
     * Retrieves the value associated with the specified key from the concurrent hash map.
     *
     * @param key The key whose associated value is to be retrieved.
     * @return The value to which the specified key is mapped, or null if the key is not present in the map.
     */
    public Integer get(String key) {
        return map.get(key);
    }

    /**
     * Checks whether the concurrent hash map contains the specified key.
     *
     * @param key The key whose presence in the map is to be tested.
     * @return true if the map contains a mapping for the specified key, otherwise false.
     */
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }
}