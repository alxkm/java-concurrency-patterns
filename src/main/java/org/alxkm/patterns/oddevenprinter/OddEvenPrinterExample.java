package org.alxkm.patterns.oddevenprinter;

/**
 * The OddEvenPrinterExample class demonstrates printing odd and even numbers in separate threads without a dedicated printer class.
 * It synchronizes the printing process to ensure that odd and even numbers are printed in alternating order.
 */
public class OddEvenPrinterExample {
    private static final Object lock = new Object();
    private static boolean isOddTurn = true;
    private static final int limit = 10;

    /**
     * Main method to start the odd and even threads for printing numbers.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        Thread oddThread = new Thread(OddEvenPrinterExample::printOddNumbers);
        Thread evenThread = new Thread(OddEvenPrinterExample::printEvenNumbers);

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
