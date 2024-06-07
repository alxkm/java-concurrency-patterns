package org.alxkm.patterns.future;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FutureExampleTest {

    /**
     * This test method verifies the functionality of the FutureResult pattern, which allows
     * asynchronous execution of tasks and retrieval of their results. It creates a single-threaded
     * executor service and submits a task for execution. The test then waits for the task to complete
     * and retrieves the result using the Future object. Finally, the test asserts that the result obtained
     * from the Future matches the expected value (123) and shuts down the executor service.
     */
    @Test
    public void testFutureResult() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Integer> future = executorService.submit(new Task());

        assertEquals(123, future.get());
        executorService.shutdown();
    }
}
