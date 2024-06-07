package org.alxkm.patterns.synchronizers;

import java.util.concurrent.Exchanger;

/**
 * Demonstrates the usage of Exchanger for exchanging data between two threads.
 */
public class ExchangerExample {
    private final Exchanger<String> exchanger = new Exchanger<>();

    /**
     * Starts the data exchange process between two threads.
     */
    public void start() throws InterruptedException {
        Thread producer = new Thread(new Producer());
        Thread consumer = new Thread(new Consumer());

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
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
                String response = exchanger.exchange(data);
                System.out.println("Producer received: " + response);
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
                String response = exchanger.exchange(data);
                System.out.println("Consumer received: " + response);
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
