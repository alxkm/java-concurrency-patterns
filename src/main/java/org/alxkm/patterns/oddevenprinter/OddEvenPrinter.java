package org.alxkm.patterns.oddevenprinter;

/**
 * The OddEvenPrinter class demonstrates printing odd and even numbers in separate threads.
 * It synchronizes the printing process to ensure that odd and even numbers are printed in alternating order.
 * <p>
 * The two printer threads share a single monitor and take turns on the {@code isOddTurn} flag: each one waits
 * while it is not its turn, records its number, flips the flag and wakes the other. {@code notifyAll} is used
 * rather than {@code notify} because it stays correct if further waiters are ever added to the same monitor.
 */
public class OddEvenPrinter {
    private static final int DEFAULT_LIMIT = 10;

    private final Object lock = new Object();
    private final int limit;
    private final StringBuilder printedOutput = new StringBuilder();
    private boolean isOddTurn = true;

    /**
     * Creates a printer that counts up to the default limit of {@value #DEFAULT_LIMIT}.
     */
    public OddEvenPrinter() {
        this(DEFAULT_LIMIT);
    }

    /**
     * Creates a printer that counts up to the given limit.
     *
     * @param limit the highest number to print, inclusive; must not be negative
     */
    public OddEvenPrinter(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative, but was " + limit);
        }
        this.limit = limit;
    }

    /**
     * Starts the threads for printing odd and even numbers and blocks until both have finished.
     */
    public void startPrinting() {
        Thread oddThread = new Thread(this::printOddNumbers, "OddThread");
        Thread evenThread = new Thread(this::printEvenNumbers, "EvenThread");

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
        print(1, false);
    }

    private void printEvenNumbers() {
        print(2, true);
    }

    /**
     * Prints every second number starting at {@code start}, waiting for this thread's turn before each one.
     * <p>
     * The whole loop sits inside a single try block on purpose. Catching {@link InterruptedException} per
     * iteration and continuing would leave the interrupt flag set, so the next {@code wait()} would throw
     * immediately and the loop would spin at full speed while silently skipping numbers. Interruption is a
     * request to stop, so the thread restores the flag and returns.
     *
     * @param start     the first number this thread is responsible for
     * @param waitWhile the value of {@code isOddTurn} that means it is not this thread's turn yet
     */
    private void print(int start, boolean waitWhile) {
        synchronized (lock) {
            try {
                for (int i = start; i <= limit; i += 2) {
                    while (isOddTurn == waitWhile) {
                        lock.wait();
                    }
                    printedOutput.append(Thread.currentThread().getName()).append(": ").append(i).append("\n");
                    isOddTurn = !isOddTurn;
                    lock.notifyAll();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Retrieves the printed output containing the odd and even numbers.
     * <p>
     * Read under the same lock that guards the writes. StringBuilder is not thread-safe, so reading
     * it while a printer thread is still appending could observe a torn value -- and the two printer
     * threads are the only writers, so the lock they already hold is the right guard.
     *
     * @return the printed output
     */
    public String getPrintedOutput() {
        synchronized (lock) {
            return printedOutput.toString();
        }
    }
}
