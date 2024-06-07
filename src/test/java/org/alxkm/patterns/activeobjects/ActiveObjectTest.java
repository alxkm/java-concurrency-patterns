package org.alxkm.patterns.activeobjects;

import org.junit.jupiter.api.Test;
import org.alxkm.patterns.activeobject.ActiveObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActiveObjectTest {

    /**
     * This test method verifies the behavior of the ActiveObject class, which demonstrates the usage of the
     * active object design pattern. It creates an ActiveObject instance and invokes the sayHello method on it
     * with the argument "World". The method call returns a Future object representing the result of the computation.
     * The test verifies that the result obtained from the Future matches the expected string "Hello, World".
     * Finally, the active object is shut down to release any associated resources.
     */
    @Test
    public void testActiveObject() throws ExecutionException, InterruptedException {
        ActiveObject activeObject = new ActiveObject();
        Future<String> future = activeObject.sayHello("World");

        assertEquals("Hello, World", future.get());
        activeObject.shutdown();
    }
}

