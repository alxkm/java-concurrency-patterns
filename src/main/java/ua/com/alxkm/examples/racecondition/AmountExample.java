package ua.com.alxkm.examples.racecondition;

import java.util.stream.IntStream;

public class AmountExample {
    public static void main(String[] args) {
        unsafeIncrement();
        safeSynchronizedIncrement();
        safeLockIncrement();
        safeAtomicIncrement();
    }

    private static void unsafeIncrement() {
        AccountAmount accountAmount = new AccountAmount();
        Runnable incrementTask = () -> IntStream.range(0, 100).forEach(i -> accountAmount.unsafeIncrementAmount());

        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Unsafe increment = " + accountAmount.getAmount());
    }

    private static void safeSynchronizedIncrement() {
        AccountAmount accountAmount = new AccountAmount();
        Runnable incrementTask = () -> IntStream.range(0, 100).forEach(i -> accountAmount.safeSynchronizedIncrementAmount());

        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Safe synchronized increment = " + accountAmount.getAmount());
    }

    private static void safeLockIncrement() {
        AccountAmount accountAmount = new AccountAmount();
        Runnable incrementTask = () -> IntStream.range(0, 100).forEach(i -> accountAmount.safeLockIncrementAmount());

        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Safe lock increment = " + accountAmount.getAmount());
    }

    private static void safeAtomicIncrement() {
        AccountAmount accountAmount = new AccountAmount();
        Runnable incrementTask = () -> IntStream.range(0, 100).forEach(i -> accountAmount.incrementAtomicAmount());

        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Safe atomic increment = " + accountAmount.getAtomicInteger());
    }
}
