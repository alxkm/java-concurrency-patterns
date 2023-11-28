package ua.com.alxkm.patterns.barrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BarrierExample {
    private static final int THREAD_COUNT = 3;
    private static final CyclicBarrier BARRIER = new CyclicBarrier(THREAD_COUNT, () -> System.out.println("All threads have reached the barrier, continuing..."));

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            System.out.println(Thread.currentThread().getName() + " started");
            try {
                // Simulating some work
                Thread.sleep(1000);

                System.out.println(Thread.currentThread().getName() + " is waiting at the barrier");
                // The await() method causes the current thread to wait until all threads reach this point
                BARRIER.await();
                System.out.println(Thread.currentThread().getName() + " passed the barrier");

                // More work after passing the barrier
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        };

        // Starting THREAD_COUNT threads
        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(task).start();
        }
    }
}
