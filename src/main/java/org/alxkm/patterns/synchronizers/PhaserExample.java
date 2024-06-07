package org.alxkm.patterns.synchronizers;

import java.util.concurrent.Phaser;

/**
 * The CustomTask class represents a task to be executed in the PhaserExample.
 * Each task arrives at the phaser and then deregisters from it upon completion.
 */
class CustomTask implements Runnable {
    private final Phaser phaser;
    private final String taskName;

    /**
     * Constructs a CustomTask with the given phaser and task name.
     *
     * @param phaser   the phaser to synchronize task execution
     * @param taskName the name of the task
     */
    public CustomTask(Phaser phaser, String taskName) {
        this.phaser = phaser;
        this.taskName = taskName;
        phaser.register();
    }

    @Override
    public void run() {
        System.out.println(taskName + " started");
        phaser.arrive(); // Indicate that this task has arrived
        System.out.println(taskName + " completed");
        phaser.arriveAndDeregister(); // Indicate that this task has completed and deregister from the phaser
    }
}

/**
 * The PhaserExample class demonstrates the usage of Phaser to synchronize multiple tasks.
 * It creates multiple threads, each representing a task, and waits for all tasks to complete using a Phaser.
 */
public class PhaserExample {
    public static void main(String[] args) {
        final int TASK_COUNT = 3;
        Phaser phaser = new Phaser(TASK_COUNT); // Create a Phaser with the specified number of parties

        // Create and start threads for each task
        for (int i = 1; i <= TASK_COUNT; i++) {
            String taskName = "Task " + i;
            new Thread(new CustomTask(phaser, taskName)).start();
        }
    }
}