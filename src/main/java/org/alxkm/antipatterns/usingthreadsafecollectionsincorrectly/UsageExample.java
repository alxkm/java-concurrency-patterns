package org.alxkm.antipatterns.usingthreadsafecollectionsincorrectly;

public class UsageExample {
    public static <T> void runExample(BaseListUsage<T> usage) {

        // Create threads to perform operations on the list
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                usage.addIfAbsent("Element " + i);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 50; i < 150; i++) {
                usage.addIfAbsent("Element " + i);
            }
        });

        // Start the threads
        thread1.start();
        thread2.start();

        // Wait for both threads to complete
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Print the final size of the list
        System.out.println("Final size of the list: " + usage.size());

        // Print elements in the list to demonstrate correct usage
        System.out.println("Elements in the list:");
        for (T element : usage.getCollection()) {
            System.out.println(element);
        }
    }

    public static void main(String[] args) {
        CorrectUsage correctUsage = new CorrectUsage();
        IncorrectUsage incorrectUsage = new IncorrectUsage();
        OptimizedUsage optimizedUsage = new OptimizedUsage();
        runExample(correctUsage);
        runExample(incorrectUsage);
        runExample(optimizedUsage);
    }
}
