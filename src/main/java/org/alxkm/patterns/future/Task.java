package org.alxkm.patterns.future;

import java.util.concurrent.Callable;

/**
 * The Task class implements the Callable interface and represents a task
 * that computes a result asynchronously.
 */
class Task implements Callable<Integer> {

    /**
     * The call method is called by the ExecutorService to execute the task.
     * It returns the result of the computation.
     *
     * @return The result of the computation.
     * @throws Exception If an exception occurs during the computation.
     */
    @Override
    public Integer call() throws Exception {
        return 123;
    }
}
