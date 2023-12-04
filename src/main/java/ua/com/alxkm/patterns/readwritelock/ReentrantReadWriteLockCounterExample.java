package ua.com.alxkm.patterns.readwritelock;

public class ReentrantReadWriteLockCounterExample {
    public static void main(String[] args) {
        ReentrantReadWriteLockCounter counter = new ReentrantReadWriteLockCounter();
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.incrementCounter();
                }
            });
            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int finalCounterValue = counter.getCounter();
        System.out.println("Final Counter Value: " + finalCounterValue);
    }
}
