package org.alxkm.antipatterns.racecondition;

import java.util.stream.IntStream;

/**
 * A class demonstrating different methods of incrementing an account amount with various synchronization techniques.
 */
public class AccountExample {

    /**
     * The main method demonstrating different methods of incrementing an account amount.
     *
     * @param args The command-line arguments (unused).
     */
    public static void main(String[] args) {
        unsafeIncrement();
        safeSynchronizedIncrement();
        safeLockIncrement();
        safeAtomicIncrement();
    }

    /**
     * Demonstrates unsafe incrementing of an account amount without synchronization.
     */
    private static void unsafeIncrement() {
        // Setup
        AccountAmount accountAmount = new AccountAmount();
        Runnable incrementTask = () -> IntStream.range(0, 100).forEach(i -> accountAmount.unsafeIncrementAmount());

        // Thread creation
        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);

        // Thread execution
        t1.start();
        t2.start();

        // Wait for threads to finish
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Output result
        System.out.println("Unsafe increment = " + accountAmount.getAmount());
    }

    /**
     * Demonstrates safe synchronized incrementing of an account amount.
     */
    private static void safeSynchronizedIncrement() {
        // Setup
        AccountAmount accountAmount = new AccountAmount();
        Runnable incrementTask = () -> IntStream.range(0, 100).forEach(i -> accountAmount.safeSynchronizedIncrementAmount());

        // Thread creation
        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);

        // Thread execution
        t1.start();
        t2.start();

        // Wait for threads to finish
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Output result
        System.out.println("Safe synchronized increment = " + accountAmount.getAmount());
    }

    /**
     * Demonstrates safe lock-based incrementing of an account amount.
     */
    private static void safeLockIncrement() {
        // Setup
        AccountAmount accountAmount = new AccountAmount();
        Runnable incrementTask = () -> IntStream.range(0, 100).forEach(i -> accountAmount.safeLockIncrementAmount());

        // Thread creation
        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);

        // Thread execution
        t1.start();
        t2.start();

        // Wait for threads to finish
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Output result
        System.out.println("Safe lock increment = " + accountAmount.getAmount());
    }

    /**
     * Demonstrates safe atomic incrementing of an account amount.
     */
    private static void safeAtomicIncrement() {
        // Setup
        AccountAmount accountAmount = new AccountAmount();
        Runnable incrementTask = () -> IntStream.range(0, 100).forEach(i -> accountAmount.incrementAtomicAmount());

        // Thread creation
        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);

        // Thread execution
        t1.start();
        t2.start();

        // Wait for threads to finish
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Output result
        System.out.println("Safe atomic increment = " + accountAmount.getAtomicInteger());
    }
}
