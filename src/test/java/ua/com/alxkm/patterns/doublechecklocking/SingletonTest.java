package ua.com.alxkm.patterns.doublechecklocking;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SingletonTest {

    private static final int THREAD_COUNT = 1000;

    @Test
    public void testSingletonMultithreaded() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        Singleton[] instances = new Singleton[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            int index = i;
            executor.execute(() -> {
                instances[index] = Singleton.getInstance();
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        for (int i = 1; i < THREAD_COUNT; i++) {
            assertSame(instances[0], instances[i], "All instances should be the same");
        }
    }
}