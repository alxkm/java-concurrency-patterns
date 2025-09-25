package org.alxkm.patterns.builder;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-Safe Builder Pattern implementation.
 * 
 * This example demonstrates how to create a thread-safe builder pattern
 * using synchronization mechanisms to ensure that the builder can be
 * safely used across multiple threads.
 */
public class ThreadSafeBuilder {
    private final ReentrantLock lock = new ReentrantLock();
    
    // Target object to build
    public static class Product {
        private final String name;
        private final int value;
        private final boolean enabled;
        private final String description;
        
        private Product(String name, int value, boolean enabled, String description) {
            this.name = name;
            this.value = value;
            this.enabled = enabled;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public int getValue() {
            return value;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return "Product{" +
                    "name='" + name + '\'' +
                    ", value=" + value +
                    ", enabled=" + enabled +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
    
    // Builder class with thread safety
    public static class Builder {
        private final ReentrantLock lock = new ReentrantLock();
        private String name;
        private int value;
        private boolean enabled;
        private String description;
        private boolean built = false;
        
        public Builder setName(String name) {
            lock.lock();
            try {
                if (built) {
                    throw new IllegalStateException("Builder has already been used to build a product");
                }
                this.name = name;
                return this;
            } finally {
                lock.unlock();
            }
        }
        
        public Builder setValue(int value) {
            lock.lock();
            try {
                if (built) {
                    throw new IllegalStateException("Builder has already been used to build a product");
                }
                this.value = value;
                return this;
            } finally {
                lock.unlock();
            }
        }
        
        public Builder setEnabled(boolean enabled) {
            lock.lock();
            try {
                if (built) {
                    throw new IllegalStateException("Builder has already been used to build a product");
                }
                this.enabled = enabled;
                return this;
            } finally {
                lock.unlock();
            }
        }
        
        public Builder setDescription(String description) {
            lock.lock();
            try {
                if (built) {
                    throw new IllegalStateException("Builder has already been used to build a product");
                }
                this.description = description;
                return this;
            } finally {
                lock.unlock();
            }
        }
        
        public Product build() {
            lock.lock();
            try {
                if (built) {
                    throw new IllegalStateException("Builder has already been used to build a product");
                }
                built = true;
                return new Product(name, value, enabled, description);
            } finally {
                lock.unlock();
            }
        }
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    /**
     * Example usage demonstrating thread-safe builder pattern.
     */
    public static void main(String[] args) throws InterruptedException {
        // Example 1: Basic usage
        Product product1 = ThreadSafeBuilder.newBuilder()
                .setName("Product A")
                .setValue(100)
                .setEnabled(true)
                .setDescription("A sample product")
                .build();
        
        System.out.println("Product 1: " + product1);
        
        // Example 2: Concurrent building with multiple threads
        final int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    Builder builder = ThreadSafeBuilder.newBuilder();
                    Product product = builder
                            .setName("Product-" + threadId)
                            .setValue(threadId * 10)
                            .setEnabled(threadId % 2 == 0)
                            .setDescription("Product created by thread " + threadId)
                            .build();
                    
                    System.out.println("Thread " + threadId + " created: " + product);
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " failed: " + e.getMessage());
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Example 3: Attempting to reuse builder (should throw exception)
        try {
            Builder builder = ThreadSafeBuilder.newBuilder()
                    .setName("First Product")
                    .setValue(50);
            
            Product firstProduct = builder.build();
            System.out.println("First product: " + firstProduct);
            
            // This should throw an exception
            builder.setName("Second Product").build();
        } catch (IllegalStateException e) {
            System.out.println("Expected exception: " + e.getMessage());
        }
    }
}