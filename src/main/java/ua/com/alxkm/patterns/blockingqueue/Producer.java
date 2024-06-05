package ua.com.alxkm.patterns.blockingqueue;

import java.util.concurrent.BlockingQueue;

/**
 * The Producer class represents a producer thread that produces messages and puts them into a blocking queue.
 * It continuously puts messages into the queue until interrupted.
 */
public class Producer implements Runnable {
    protected BlockingQueue<String> queue;

    /**
     * Constructs a Producer with the specified blocking queue.
     *
     * @param queue The blocking queue into which messages will be produced.
     */
    public Producer(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    /**
     * The run method of the Producer thread. It continuously produces messages and puts them into the blocking queue
     * until interrupted.
     */
    @Override
    public void run() {
        try {
            for (int i = 1; i <= 5; i++) {
                queue.put(Integer.valueOf(i).toString());
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}