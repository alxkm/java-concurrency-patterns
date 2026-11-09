package org.alxkm.patterns.synchronizers;

import java.util.concurrent.Exchanger;

/**
 * Demonstrates the usage of Exchanger for exchanging data between two threads.
 * <p>
 * An Exchanger is a meeting point for exactly two threads. Each calls exchange() with what it has, the
 * first to arrive waits for the other, and each leaves with what the other brought.
 */
public class ExchangerExample {
    private final Exchanger<String> exchanger = new Exchanger<>();

    // Each is written by one thread and read by the caller after join(), which makes the write visible.
    private String producerReceived;
    private String consumerReceived;

    /**
     * What each side came away with.
     *
     * @param producerReceived what the producer got from the consumer.
     * @param consumerReceived what the consumer got from the producer.
     */
    public record Exchange(String producerReceived, String consumerReceived) {
    }

    /**
     * Starts the producer and the consumer, waits for both, and reports what each one received.
     *
     * @return what each side received.
     * @throws InterruptedException if this thread is interrupted while waiting for them.
     */
    public Exchange start() throws InterruptedException {
        Thread producer = new Thread(new Producer());
        Thread consumer = new Thread(new Consumer());

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        return new Exchange(producerReceived, consumerReceived);
    }

    /**
     * Producer class that exchanges data with the Consumer.
     */
    private class Producer implements Runnable {
        @Override
        public void run() {
            try {
                String data = "Data from Producer";
                System.out.println("Producer: " + data);
                producerReceived = exchanger.exchange(data);
                System.out.println("Producer received: " + producerReceived);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Consumer class that exchanges data with the Producer.
     */
    private class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                String data = "Data from Consumer";
                System.out.println("Consumer: " + data);
                consumerReceived = exchanger.exchange(data);
                System.out.println("Consumer received: " + consumerReceived);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ExchangerExample example = new ExchangerExample();
        example.start();
    }
}
