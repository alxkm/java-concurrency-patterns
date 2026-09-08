package org.alxkm.patterns.oddevenprinter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddEvenPrinterTest {

    /**
     * This test method verifies the functionality of the OddEvenPrinter class, which implements a
     * solution to the classic Odd-Even printing problem using two separate threads. It starts the
     * printing process by invoking the startPrinting method of the OddEvenPrinter instance, which
     * joins both printer threads before returning -- so there is nothing left to wait for and the
     * output can be compared immediately. The expected output contains alternating lines printed
     * by the OddThread and EvenThread threads, starting from 1. This test ensures that the OddEvenPrinter
     * class correctly prints odd and even numbers in sequence using two separate threads.
     */
    @Test
    void testOddEvenPrinter() {
        OddEvenPrinter oddEvenPrinter = new OddEvenPrinter();

        // startPrinting() joins both threads, so it returns only once printing is complete.
        oddEvenPrinter.startPrinting();

        String expectedOutput =
                "OddThread: 1\n" +
                "EvenThread: 2\n" +
                "OddThread: 3\n" +
                "EvenThread: 4\n" +
                "OddThread: 5\n" +
                "EvenThread: 6\n" +
                "OddThread: 7\n" +
                "EvenThread: 8\n" +
                "OddThread: 9\n" +
                "EvenThread: 10";
        assertEquals(expectedOutput, oddEvenPrinter.getPrintedOutput().trim());
    }

    /**
     * Both odd and even limits must terminate: whichever thread runs out of numbers first stops handing the
     * turn over, so a printer that relied on the other thread signalling it again would hang here.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 11})
    void printsEveryNumberUpToTheLimitInOrder(int limit) {
        OddEvenPrinter printer = new OddEvenPrinter(limit);

        printer.startPrinting();

        StringBuilder expected = new StringBuilder();
        for (int i = 1; i <= limit; i++) {
            expected.append(i % 2 == 1 ? "OddThread: " : "EvenThread: ").append(i).append("\n");
        }
        assertEquals(expected.toString(), printer.getPrintedOutput());
    }

    @Test
    void emptyOutputWhenThereIsNothingToPrint() {
        OddEvenPrinter printer = new OddEvenPrinter(0);

        printer.startPrinting();

        assertTrue(printer.getPrintedOutput().isEmpty());
    }

    @Test
    void rejectsNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> new OddEvenPrinter(-1));
    }
}
