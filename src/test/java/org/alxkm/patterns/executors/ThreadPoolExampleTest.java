package org.alxkm.patterns.executors;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThreadPoolExampleTest {

    /**
     * This test method verifies the execution of tasks in a thread pool using
     * the ExecutorService. It submits 20 tasks to a fixed thread pool of size 10,
     * each task simply decrements a CountDownLatch. It then shuts down the
     * executor service and waits for all tasks to complete within 1 second,
     * ensuring that all tasks executed successfully.
     */
    @Test
    public void testThreadPoolExecution() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(20);

        for (int i = 0; i < 20; i++) {
            executorService.execute(() -> {
                latch.countDown();
            });
        }
        executorService.shutdown();
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
