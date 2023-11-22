package ua.com.alxkm.examples.oddevenprinter;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OddEvenPrinterTest {
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
