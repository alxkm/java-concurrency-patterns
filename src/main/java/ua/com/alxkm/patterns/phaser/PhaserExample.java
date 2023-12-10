package ua.com.alxkm.patterns.phaser;

import java.util.concurrent.Phaser;

class CustomTask implements Runnable {
    private final Phaser phaser;
    private final String taskName;

    public CustomTask(Phaser phaser, String taskName) {
        this.phaser = phaser;
        this.taskName = taskName;
        phaser.register();
    }

    @Override
    public void run() {
        System.out.println(taskName + " started");
        phaser.arrive();
        System.out.println(taskName + " completed");
        phaser.arriveAndDeregister();
    }
}

public class PhaserExample {
    public static void main(String[] args) {
        final int TASK_COUNT = 3;
        Phaser phaser = new Phaser(TASK_COUNT);

        for (int i = 1; i <= TASK_COUNT; i++) {
            String taskName = "Task " + i;
            new Thread(new CustomTask(phaser, taskName)).start();
        }
    }
}