package ua.com.alxkm.patterns.blockingqueue;

import java.util.concurrent.BlockingQueue;

public class Producer implements Runnable {
    protected BlockingQueue<String> queue;

    public Producer(BlockingQueue<String> queue) {
        this.queue = queue;
    }

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