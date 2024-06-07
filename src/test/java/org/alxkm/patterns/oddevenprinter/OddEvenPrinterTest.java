package org.alxkm.patterns.oddevenprinter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OddEvenPrinterTest {

    /**
     * This test method verifies the functionality of the OddEvenPrinter class, which implements a
     * solution to the classic Odd-Even printing problem using two separate threads. It starts the
     * printing process by invoking the startPrinting method of the OddEvenPrinter instance. After
     * allowing some time for the threads to finish printing, the test compares the printed output
     * against the expected output. The expected output contains alternating lines of numbers printed
     * by the OddThread and EvenThread threads, starting from 1. This test ensures that the OddEvenPrinter
     * class correctly prints odd and even numbers in sequence using two separate threads.
     */
    @Test
    public void testOddEvenPrinter() throws InterruptedException {
        OddEvenPrinter oddEvenPrinter = new OddEvenPrinter();
        oddEvenPrinter.startPrinting();

        // Sleep for a while to allow threads to finish printing
        Thread.sleep(1000);

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
}
