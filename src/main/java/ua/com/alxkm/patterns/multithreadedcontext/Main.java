package ua.com.alxkm.patterns.multithreadedcontext;

public class Main {
    public static void main(String[] args) {
        MultithreadedContext context = MultithreadedContext.getInstance();

        Runnable operation = () -> {
            System.out.println("Thread-safe operation is performed.");
        };

        context.apply(operation);
    }
}
