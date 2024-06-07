package org.alxkm.patterns.activeobject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The ActiveObject class decouples method execution from method invocation to enhance concurrency.
 * It encapsulates method calls as objects, allowing them to be executed asynchronously in a separate
 * thread without blocking the calling thread. This pattern is useful for improving responsiveness
 * and resource utilization in concurrent applications.
 */
public class ActiveObject {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Asynchronously invokes the sayHello method with the given name.
     *
     * @param name The name to include in the greeting message.
     * @return A Future representing the result of the computation.
     */
    public Future<String> sayHello(String name) {
        return executor.submit(() -> {
            Thread.sleep(100); // Simulate delay
            return "Hello, " + name;
        });
    }

    /**
     * Shuts down the executor service associated with this active object, releasing any resources.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
