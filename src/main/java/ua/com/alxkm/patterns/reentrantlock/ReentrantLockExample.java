package ua.com.alxkm.patterns.reentrantlock;

public class ReentrantLockExample {
    public static void main(String[] args) throws InterruptedException {
        ReentrantLockCounter reentrantLockCounter = new ReentrantLockCounter();

        for (int i = 0; i < 100; i++) {
            new Thread(reentrantLockCounter::incrementCounter).start();
        }
        Thread.sleep(5000);
        System.out.println(reentrantLockCounter.getCounter());
    }
}
