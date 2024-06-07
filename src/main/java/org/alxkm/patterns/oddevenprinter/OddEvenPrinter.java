package org.alxkm.patterns.oddevenprinter;

/**
 * The OddEvenPrinter class demonstrates printing odd and even numbers in separate threads.
 * It synchronizes the printing process to ensure that odd and even numbers are printed in alternating order.
 */
public class OddEvenPrinter {
    private final Object lock = new Object();
    private boolean isOddTurn = true;
    private final int limit = 10;
    private final StringBuilder printedOutput = new StringBuilder();

    /**
     * Starts the threads for printing odd and even numbers.
     */
    public void startPrinting() {
        Thread oddThread = new Thread(this::printOddNumbers);
        Thread evenThread = new Thread(this::printEvenNumbers);

        oddThread.setName("OddThread");
        evenThread.setName("EvenThread");

        evenThread.start();
        oddThread.start();

        try {
            oddThread.join();
            evenThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void printOddNumbers() {
        synchronized (lock) {
            for (int i = 1; i <= limit; i += 2) {
                try {
                    while (!isOddTurn) {
                        lock.wait();
                    }
                    printedOutput.append(Thread.currentThread().getName()).append(": ").append(i).append("\n");
                    isOddTurn = false;
                    lock.notify();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void printEvenNumbers() {
        synchronized (lock) {
            for (int i = 2; i <= limit; i += 2) {
                try {
                    while (isOddTurn) {
                        lock.wait();
                    }
                    printedOutput.append(Thread.currentThread().getName()).append(": ").append(i).append("\n");
                    isOddTurn = true;
                    lock.notify();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Retrieves the printed output containing the odd and even numbers.
     *
     * @return the printed output
     */
    public String getPrintedOutput() {
        return printedOutput.toString();
    }
}
