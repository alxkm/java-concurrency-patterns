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
 *
 * */
public class CorrectUsage {
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

    public static void main(String[] args) {
        CorrectUsage usage = new CorrectUsage();

        // Create threads to perform operations on the list
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                usage.addIfAbsent("Element " + i);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 50; i < 150; i++) {
                usage.addIfAbsent("Element " + i);
            }
        });

        // Start the threads
        thread1.start();
        thread2.start();

        // Wait for both threads to complete
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Print the final size of the list
        System.out.println("Final size of the list: " + usage.size());

        // Print elements in the list to demonstrate correct usage
        System.out.println("Elements in the list:");
        for (String element : usage.list) {
            System.out.println(element);
        }
    }
}
