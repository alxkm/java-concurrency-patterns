package org.alxkm.patterns.oddevenprinter;

/**
 * The OddEvenPrinterExample class demonstrates printing odd and even numbers in separate threads without a dedicated printer class.
 * It synchronizes the printing process to ensure that odd and even numbers are printed in alternating order.
 * <p>
 * See {@link OddEvenPrinter} for the same handover expressed as a reusable, testable object.
 */
public class OddEvenPrinterExample {
    private static final Object LOCK = new Object();
    private static final int LIMIT = 10;

    private static boolean isOddTurn = true;

    /**
     * Main method to start the odd and even threads for printing numbers.
     * It waits for both threads so the program ends only once the whole sequence has been printed.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        Thread oddThread = new Thread(OddEvenPrinterExample::printOddNumbers, "OddThread");
        Thread evenThread = new Thread(OddEvenPrinterExample::printEvenNumbers, "EvenThread");

        evenThread.start();
        oddThread.start();

        try {
            // Wait for both threads to complete
            oddThread.join();
            evenThread.join();
            System.out.println("Both threads completed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread was interrupted");
        }
    }

    private static void printOddNumbers() {
        print(1, false);
    }

    private static void printEvenNumbers() {
        print(2, true);
    }

    /**
     * Prints every second number starting at {@code start}, waiting for this thread's turn before each one.
     * <p>
     * The loop as a whole is guarded by the try block: catching {@link InterruptedException} per iteration and
     * carrying on would leave the interrupt flag set, so the next {@code wait()} would throw straight away and
     * the loop would spin at full speed while skipping numbers. Interruption means stop, so the thread restores
     * the flag and returns.
     *
     * @param start     the first number this thread is responsible for
     * @param waitWhile the value of {@code isOddTurn} that means it is not this thread's turn yet
     */
    private static void print(int start, boolean waitWhile) {
        synchronized (LOCK) {
            try {
                for (int i = start; i <= LIMIT; i += 2) {
                    while (isOddTurn == waitWhile) {
                        LOCK.wait();
                    }
                    System.out.println(Thread.currentThread().getName() + ": " + i);
                    isOddTurn = !isOddTurn;
                    LOCK.notifyAll();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
