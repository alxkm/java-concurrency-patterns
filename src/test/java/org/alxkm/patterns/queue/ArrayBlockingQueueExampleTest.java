package org.alxkm.patterns.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ArrayBlockingQueueExampleTest {

    /**
     * This test method verifies the behavior of the ArrayBlockingQueueExample class, which demonstrates
     * the usage of the ArrayBlockingQueue class for producer-consumer synchronization. It creates a producer
     * thread that adds integers to the blocking queue and a consumer thread that consumes integers from the queue.
     * The producer thread produces integers from 0 to 9, while the consumer thread consumes them. The test ensures
     * that each produced integer is successfully consumed from the queue and that no null values are consumed,
     * indicating that the producer-consumer synchronization is working correctly.
     */
    @Test
    public void testArrayBlockingQueue() throws InterruptedException {
        ArrayBlockingQueueExample example = new ArrayBlockingQueueExample();
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    example.produce(i);
                    System.out.println("Produced: " + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Integer value = example.consume();
                    System.out.println("Consumed: " + value);
                    assertNotNull(value);
                }
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
