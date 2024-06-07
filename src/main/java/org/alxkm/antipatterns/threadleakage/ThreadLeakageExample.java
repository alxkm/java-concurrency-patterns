package org.alxkm.antipatterns.threadleakage;

/**
 * Demonstrates thread leakage by continuously creating and starting new threads without proper termination.
 */
public class ThreadLeakageExample {

    public static void main(String[] args) {
        ThreadLeakageExample example = new ThreadLeakageExample();
        example.startThreads();
    }

    /**
     * Continuously starts new threads without properly managing their lifecycle, leading to thread leakage.
     */
    public void startThreads() {
        while (true) {
            new Thread(() -> {
                // Simulate some work with a sleep
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}

