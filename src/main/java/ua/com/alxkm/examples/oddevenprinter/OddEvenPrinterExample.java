package ua.com.alxkm.examples.oddevenprinter;

public class OddEvenPrinterExample {
    private static final Object lock = new Object();
    private static boolean isOddTurn = true;
    private static final int limit = 10;

    public static void main(String[] args) {
        Thread oddThread = new Thread(() -> printOddNumbers());
        Thread evenThread = new Thread(() -> printEvenNumbers());

        oddThread.setName("OddThread");
        evenThread.setName("EvenThread");

        evenThread.start();
        oddThread.start();
    }

    private static void printOddNumbers() {
        synchronized (lock) {
            for (int i = 1; i <= limit; i += 2) {
                try {
                    while (!isOddTurn) {
                        lock.wait();
                    }
                    System.out.println(Thread.currentThread().getName() + ": " + i);
                    isOddTurn = false;
                    lock.notify();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void printEvenNumbers() {
        synchronized (lock) {
            for (int i = 2; i <= limit; i += 2) {
                try {
                    while (isOddTurn) {
                        lock.wait();
                    }
                    System.out.println(Thread.currentThread().getName() + ": " + i);
                    isOddTurn = true;
                    lock.notify();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
