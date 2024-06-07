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
     */
    public static void atomicBooleanExample() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        System.out.println("Initial value of AtomicBoolean: " + atomicBoolean.get());

        // Atomic compare-and-set operation
        boolean oldValue = atomicBoolean.getAndSet(false);
        System.out.println("Old value of AtomicBoolean: " + oldValue);
        System.out.println("New value of AtomicBoolean: " + atomicBoolean.get());
    }

    /**
     * Demonstrates the usage of AtomicInteger.
     */
    public static void atomicIntegerExample() {
        AtomicInteger atomicInteger = new AtomicInteger(10);
        System.out.println("Initial value of AtomicInteger: " + atomicInteger.get());

        // Atomic increment operation
        int newValue = atomicInteger.incrementAndGet();
        System.out.println("New value after increment: " + newValue);
    }

    /**
     * Demonstrates the usage of AtomicLong.
     */
    public static void atomicLongExample() {
        AtomicLong atomicLong = new AtomicLong(100);
        System.out.println("Initial value of AtomicLong: " + atomicLong.get());

        // Atomic decrement operation
        long newValue = atomicLong.decrementAndGet();
        System.out.println("New value after decrement: " + newValue);
    }

    /**
     * Demonstrates the usage of AtomicIntegerArray.
     */
    public static void atomicIntegerArrayExample() {
        int[] values = {1, 2, 3};
        AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(values);
        System.out.println("Initial values of AtomicIntegerArray: " + atomicIntegerArray);

        // Atomic add operation to a specific index
        atomicIntegerArray.getAndAdd(1, 5);
        System.out.println("Updated values of AtomicIntegerArray: " + atomicIntegerArray);
    }

    /**
     * Demonstrates the usage of AtomicLongArray.
     */
    public static void atomicLongArrayExample() {
        long[] values = {100, 200, 300};
        AtomicLongArray atomicLongArray = new AtomicLongArray(values);
        System.out.println("Initial values of AtomicLongArray: " + atomicLongArray);

        // Atomic compare-and-set operation to a specific index
        boolean updated = atomicLongArray.compareAndSet(1, 200, 250);
        if (updated) {
            System.out.println("Updated values of AtomicLongArray: " + atomicLongArray);
        } else {
            System.out.println("Value was not updated");
        }
    }

    public static void main(String[] args) {
        atomicBooleanExample();
        atomicIntegerExample();
        atomicLongExample();
        atomicIntegerArrayExample();
        atomicLongArrayExample();
    }
}

