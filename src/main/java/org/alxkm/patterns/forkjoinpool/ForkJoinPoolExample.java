package org.alxkm.patterns.forkjoinpool;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class ForkJoinPoolExample {

    public static void main(String[] args) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4); // 4 parallel threads
        MyRecursiveTask task = new MyRecursiveTask(100);
        Integer result = forkJoinPool.invoke(task);
        System.out.println("Result: " + result);
    }
}

class MyRecursiveTask extends RecursiveTask<Integer> {
    private int workload;

    MyRecursiveTask(int workload) {
        this.workload = workload;
    }

    @Override
    protected Integer compute() {
        if (workload > 16) {
            MyRecursiveTask subtask1 = new MyRecursiveTask(workload / 2);
            MyRecursiveTask subtask2 = new MyRecursiveTask(workload / 2);

            subtask1.fork();
            subtask2.fork();

            int result = subtask1.join() + subtask2.join();
            return result;
        } else {
            return workload * workload;
        }
    }
}
