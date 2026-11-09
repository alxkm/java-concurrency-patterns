package org.alxkm.patterns.synchronizers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for ExchangerExample.
 * <p>
 * The assertions run on the test thread. An assertion inside a spawned thread only kills that thread:
 * JUnit never hears about it, and the test passes whatever the thread saw.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ExchangerExampleTest {

    @Test
    void eachSideReceivesWhatTheOtherOffered() throws InterruptedException {
        ExchangerExample.Exchange exchange = new ExchangerExample().start();

        assertEquals("Data from Consumer", exchange.producerReceived());
        assertEquals("Data from Producer", exchange.consumerReceived());
    }

    /**
     * exchange() is a rendezvous: with no partner it waits, and with a timeout it gives up.
     */
    @Test
    void aThreadWithNoPartnerWaitsAndThenTimesOut() {
        Exchanger<String> exchanger = new Exchanger<>();

        assertThrows(TimeoutException.class, () -> exchanger.exchange("alone", 50, TimeUnit.MILLISECONDS));
    }
}
