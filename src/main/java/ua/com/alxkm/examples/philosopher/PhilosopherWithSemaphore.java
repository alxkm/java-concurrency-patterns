package ua.com.alxkm.examples.philosopher;

import java.util.concurrent.Semaphore;

public class PhilosopherWithSemaphore extends Thread {
    private final Semaphore sem;
    private boolean full = false;

    private final String name;

    PhilosopherWithSemaphore(Semaphore sem, String name) {
        this.sem = sem;
        this.name = name;
    }

    public void run() {
        try {
            if (!full) {
                sem.acquire();
                System.out.println(name + " preparing");
                sleep(300);
                full = true;
                System.out.println(name + " finished");
                sem.release();
                sleep(300);
            }
        } catch (InterruptedException e) {
            System.out.println("Error has happened");
        }
    }
}
