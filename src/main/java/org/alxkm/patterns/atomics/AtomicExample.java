package org.alxkm.patterns.atomics;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Demonstrates the usage of various atomic classes for atomic operations.
 */
public class AtomicExample {

    /**
     * Demonstrates the usage of AtomicBoolean.
     *
     * @return the value left in the AtomicBoolean once the sequence above has run.
     */
    public static boolean atomicBooleanExample() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        System.out.println("Initial value of AtomicBoolean: " + atomicBoolean.get());

        // Atomic get-and-set operation (returns the previous value)
        boolean oldValue = atomicBoolean.getAndSet(false);
        System.out.println("Old value of AtomicBoolean (from getAndSet): " + oldValue);
        System.out.println("New value of AtomicBoolean: " + atomicBoolean.get());

        // Compare-and-set (CAS): change from false to true if currently false
        boolean cas = atomicBoolean.compareAndSet(false, true);
        System.out.println("CAS from false->true applied: " + cas + ", current: " + atomicBoolean.get());

        return atomicBoolean.get();
    }

    /**
     * Demonstrates the usage of AtomicInteger.
     *
     * @return the value left in the AtomicInteger once the sequence above has run.
     */
    public static int atomicIntegerExample() {
        AtomicInteger atomicInteger = new AtomicInteger(10);
        System.out.println("Initial value of AtomicInteger: " + atomicInteger.get());

        // Atomic increment operation
        int afterInc = atomicInteger.incrementAndGet();
        System.out.println("Value after incrementAndGet(): " + afterInc);

        // Atomic add operation
        int afterAdd = atomicInteger.addAndGet(5);
        System.out.println("Value after addAndGet(5): " + afterAdd);

        // Compare-and-set (only changes if the expected value matches)
        boolean cas = atomicInteger.compareAndSet(16, 42);
        System.out.println("compareAndSet(16->42) applied: " + cas + ", current: " + atomicInteger.get());

        return atomicInteger.get();
    }

    /**
     * Demonstrates the usage of AtomicLong.
     *
     * @return the value left in the AtomicLong once the sequence above has run.
     */
    public static long atomicLongExample() {
        AtomicLong atomicLong = new AtomicLong(100);
        System.out.println("Initial value of AtomicLong: " + atomicLong.get());

        // Atomic decrement operation
        long afterDec = atomicLong.decrementAndGet();
        System.out.println("Value after decrementAndGet(): " + afterDec);

        // Atomic update with a function (Java 8+)
        long updated = atomicLong.updateAndGet(v -> v * 2);
        System.out.println("Value after updateAndGet(v -> v * 2): " + updated);

        return atomicLong.get();
    }

    /**
     * Demonstrates the usage of AtomicIntegerArray.
     *
     * @return the array contents once the sequence above has run.
     */
    public static int[] atomicIntegerArrayExample() {
        int[] values = {1, 2, 3};
        AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(values);
        System.out.println("Initial values of AtomicIntegerArray: " + atomicIntegerArray);

        // Atomic add operation to a specific index
        int prev = atomicIntegerArray.getAndAdd(1, 5);
        System.out.println("Previous value at index 1: " + prev);
        System.out.println("Updated values of AtomicIntegerArray: " + atomicIntegerArray);

        // CAS at index 0
        boolean cas = atomicIntegerArray.compareAndSet(0, 1, 10);
        System.out.println("CAS at index 0 (1->10) applied: " + cas + ", values: " + atomicIntegerArray);

        int[] result = new int[atomicIntegerArray.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = atomicIntegerArray.get(i);
        }
        return result;
    }

    /**
     * Demonstrates the usage of AtomicLongArray.
     *
     * @return the array contents once the sequence above has run.
     */
    public static long[] atomicLongArrayExample() {
        long[] values = {100, 200, 300};
        AtomicLongArray atomicLongArray = new AtomicLongArray(values);
        System.out.println("Initial values of AtomicLongArray: " + atomicLongArray);

        // Atomic compare-and-set operation to a specific index
        boolean updated = atomicLongArray.compareAndSet(1, 200, 250);
        if (updated) {
            System.out.println("Updated values of AtomicLongArray: " + atomicLongArray);
        } else {
            System.out.println("Value at index 1 was not updated");
        }

        // Increment all elements
        for (int i = 0; i < atomicLongArray.length(); i++) {
            atomicLongArray.incrementAndGet(i);
        }
        System.out.println("After incrementing all elements: " + atomicLongArray);

        long[] result = new long[atomicLongArray.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = atomicLongArray.get(i);
        }
        return result;
    }

    public static void main(String[] args) {
        atomicBooleanExample();
        atomicIntegerExample();
        atomicLongExample();
        atomicIntegerArrayExample();
        atomicLongArrayExample();
    }
}

