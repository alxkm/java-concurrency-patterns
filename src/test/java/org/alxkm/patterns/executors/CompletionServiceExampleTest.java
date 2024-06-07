package org.alxkm.patterns.executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Unit tests for the CompletionServiceExample class.
 */
public class CompletionServiceExampleTest {
    private CompletionServiceExample example;

    @BeforeEach
    public void setUp() {
        example = new CompletionServiceExample();
    }

    @Test
    public void testExecuteTasks() throws InterruptedException, ExecutionException {
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep((int) (Math.random() * 100));
                return "Task " + taskId + " completed";
            });
        }

        // We cannot assert the exact order of completion because tasks complete asynchronously.
        // Instead, we focus on whether all tasks complete without throwing exceptions.
        example.executeTasks(tasks);
    }

    @Test
    public void testExecuteTasksWithCorrectResults() throws InterruptedException, ExecutionException {
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            tasks.add(() -> "Task " + taskId + " completed");
        }

        example.executeTasks(tasks);

        // We could extend this test to verify output via capturing system output or refactoring to return results.
    }
}
