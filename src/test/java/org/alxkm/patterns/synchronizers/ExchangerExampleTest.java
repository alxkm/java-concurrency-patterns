package org.alxkm.patterns.synchronizers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Exchanger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the ExchangerExample class.
 *
 * setUp(): Initializes a new instance of ExchangerExample before each test.
 * testExchanger(): Tests the overall exchange process by starting the producer and consumer threads.
 * testProducerConsumerExchange(): Tests the actual exchange of data between producer and consumer threads by asserting the exchanged values.
 */
public class ExchangerExampleTest {
    private ExchangerExample example;

    @BeforeEach
    public void setUp() {
        example = new ExchangerExample();
    }

    @Test
    public void testExchanger() throws InterruptedException {
        // This test indirectly verifies the exchange process by starting the producer and consumer threads
        example.start();
    }

    @Test
    public void testProducerConsumerExchange() throws InterruptedException {
        // This test verifies the actual exchange of data between producer and consumer
        Exchanger<String> exchanger = new Exchanger<>();
        Thread producer = new Thread(() -> {
            try {
                String data = "Data from Producer";
                String response = exchanger.exchange(data);
                assertEquals("Data from Consumer", response);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                String data = "Data from Consumer";
                String response = exchanger.exchange(data);
                assertEquals("Data from Producer", response);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }
}

