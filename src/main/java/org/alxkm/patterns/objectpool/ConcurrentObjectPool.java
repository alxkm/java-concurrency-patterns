package org.alxkm.patterns.objectpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Concurrent Object Pool Pattern implementation.
 * 
 * This pattern manages a pool of reusable objects to improve performance
 * and reduce garbage collection overhead. The pool is thread-safe and
 * supports dynamic object creation and validation.
 */
public class ConcurrentObjectPool<T> {
    
    private final BlockingQueue<T> pool;
    private final Supplier<T> objectFactory;
    private final ObjectValidator<T> validator;
    private final int maxPoolSize;
    private final AtomicInteger currentPoolSize;
    private final AtomicInteger createdObjectsCount;
    private final ConcurrentHashMap<T, Boolean> leasedObjects;
    private final ReentrantLock poolLock;
    
    /**
     * Validator interface for checking object validity.
     */
    public interface ObjectValidator<T> {
        boolean isValid(T object);
        void reset(T object);
    }
    
    /**
     * Default validator that considers all objects valid.
     */
    public static class DefaultValidator<T> implements ObjectValidator<T> {
        @Override
        public boolean isValid(T object) {
            return object != null;
        }
        
        @Override
        public void reset(T object) {
            // Default implementation does nothing
        }
    }
    
    /**
     * Creates a new object pool.
     * 
     * @param objectFactory Factory for creating new objects
     * @param validator Validator for checking object validity
     * @param maxPoolSize Maximum number of objects in the pool
     */
    public ConcurrentObjectPool(Supplier<T> objectFactory, ObjectValidator<T> validator, int maxPoolSize) {
        if (objectFactory == null) {
            throw new IllegalArgumentException("Object factory cannot be null");
        }
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("Max pool size must be positive");
        }
        
        this.objectFactory = objectFactory;
        this.validator = validator != null ? validator : new DefaultValidator<>();
        this.maxPoolSize = maxPoolSize;
        this.pool = new LinkedBlockingQueue<>(maxPoolSize);
        this.currentPoolSize = new AtomicInteger(0);
        this.createdObjectsCount = new AtomicInteger(0);
        this.leasedObjects = new ConcurrentHashMap<>();
        this.poolLock = new ReentrantLock();
        
        // Pre-populate pool with initial objects
        initializePool();
    }
    
    /**
     * Creates a pool with default validator.
     */
    public ConcurrentObjectPool(Supplier<T> objectFactory, int maxPoolSize) {
        this(objectFactory, null, maxPoolSize);
    }
    
    /**
     * Pre-populate the pool with some initial objects.
     */
    private void initializePool() {
        int initialSize = Math.min(maxPoolSize / 2, 5); // Start with half capacity or 5, whichever is smaller
        for (int i = 0; i < initialSize; i++) {
            T object = createNewObject();
            if (object != null) {
                pool.offer(object);
                currentPoolSize.incrementAndGet();
            }
        }
    }
    
    /**
     * Creates a new object using the factory.
     */
    private T createNewObject() {
        try {
            T object = objectFactory.get();
            createdObjectsCount.incrementAndGet();
            return object;
        } catch (Exception e) {
            System.err.println("Failed to create object: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Acquires an object from the pool.
     * 
     * @return An object from the pool, or null if unable to create/acquire
     */
    public T acquire() {
        T object = pool.poll();
        
        if (object == null) {
            // No objects in pool, try to create a new one
            poolLock.lock();
            try {
                if (currentPoolSize.get() < maxPoolSize) {
                    object = createNewObject();
                    if (object != null) {
                        currentPoolSize.incrementAndGet();
                    }
                }
            } finally {
                poolLock.unlock();
            }
        }
        
        if (object != null) {
            // Validate the object
            if (!validator.isValid(object)) {
                // Object is invalid, try to get another one
                return acquire();
            }
            
            // Mark object as leased
            leasedObjects.put(object, Boolean.TRUE);
        }
        
        return object;
    }
    
    /**
     * Acquires an object from the pool with a timeout.
     * 
     * @param timeoutMillis Maximum time to wait for an object
     * @return An object from the pool, or null if timeout expires
     */
    public T acquire(long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        T object = null;
        
        while (object == null && (System.currentTimeMillis() - startTime) < timeoutMillis) {
            object = acquire();
            if (object == null) {
                try {
                    Thread.sleep(10); // Brief wait before retrying
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return object;
    }
    
    /**
     * Returns an object to the pool.
     * 
     * @param object The object to return
     */
    public void release(T object) {
        if (object == null) {
            return;
        }
        
        // Remove from leased objects
        if (!leasedObjects.remove(object, Boolean.TRUE)) {
            // Object was not leased from this pool
            return;
        }
        
        // Reset the object if possible
        try {
            validator.reset(object);
        } catch (Exception e) {
            System.err.println("Failed to reset object: " + e.getMessage());
            // Don't return invalid object to pool
            currentPoolSize.decrementAndGet();
            return;
        }
        
        // Validate object before returning to pool
        if (!validator.isValid(object)) {
            currentPoolSize.decrementAndGet();
            return;
        }
        
        // Try to return to pool
        if (!pool.offer(object)) {
            // Pool is full, discard the object
            currentPoolSize.decrementAndGet();
        }
    }
    
    /**
     * Gets the current size of the pool.
     */
    public int getCurrentPoolSize() {
        return pool.size();
    }
    
    /**
     * Gets the total number of objects created by this pool.
     */
    public int getTotalCreatedObjects() {
        return createdObjectsCount.get();
    }
    
    /**
     * Gets the number of objects currently leased out.
     */
    public int getLeasedObjectsCount() {
        return leasedObjects.size();
    }
    
    /**
     * Gets the maximum pool size.
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    /**
     * Clears the pool and releases all resources.
     */
    public void shutdown() {
        pool.clear();
        leasedObjects.clear();
        currentPoolSize.set(0);
    }
    
    /**
     * Example poolable resource.
     */
    public static class PoolableResource {
        private final int id;
        private volatile boolean inUse;
        private long lastUsed;
        
        public PoolableResource(int id) {
            this.id = id;
            this.inUse = false;
            this.lastUsed = System.currentTimeMillis();
        }
        
        public void use() {
            inUse = true;
            lastUsed = System.currentTimeMillis();
        }
        
        public void release() {
            inUse = false;
        }
        
        public boolean isInUse() {
            return inUse;
        }
        
        public int getId() {
            return id;
        }
        
        public long getLastUsed() {
            return lastUsed;
        }
        
        @Override
        public String toString() {
            return "PoolableResource{id=" + id + ", inUse=" + inUse + "}";
        }
    }
    
    /**
     * Example validator for PoolableResource.
     */
    public static class ResourceValidator implements ObjectValidator<PoolableResource> {
        @Override
        public boolean isValid(PoolableResource resource) {
            return resource != null && !resource.isInUse();
        }
        
        @Override
        public void reset(PoolableResource resource) {
            if (resource != null) {
                resource.release();
            }
        }
    }
    
    /**
     * Example usage demonstrating concurrent object pool pattern.
     */
    public static void main(String[] args) throws InterruptedException {
        // Create a pool of resources
        AtomicInteger idGenerator = new AtomicInteger(0);
        ConcurrentObjectPool<PoolableResource> resourcePool = new ConcurrentObjectPool<>(
            () -> new PoolableResource(idGenerator.incrementAndGet()),
            new ResourceValidator(),
            10
        );
        
        System.out.println("Initial pool size: " + resourcePool.getCurrentPoolSize());
        System.out.println("Max pool size: " + resourcePool.getMaxPoolSize());
        
        // Example 1: Basic usage
        PoolableResource resource1 = resourcePool.acquire();
        if (resource1 != null) {
            System.out.println("Acquired: " + resource1);
            resource1.use();
            
            // Simulate some work
            Thread.sleep(100);
            
            resourcePool.release(resource1);
            System.out.println("Released: " + resource1);
        }
        
        // Example 2: Concurrent usage
        final int threadCount = 15;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    PoolableResource resource = resourcePool.acquire(1000); // 1 second timeout
                    if (resource != null) {
                        System.out.println("Thread " + threadId + " acquired: " + resource);
                        resource.use();
                        
                        // Simulate work
                        Thread.sleep(50);
                        
                        resourcePool.release(resource);
                        System.out.println("Thread " + threadId + " released: " + resource);
                    } else {
                        System.out.println("Thread " + threadId + " failed to acquire resource");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
        
        // Print final statistics
        System.out.println("\nFinal Statistics:");
        System.out.println("Current pool size: " + resourcePool.getCurrentPoolSize());
        System.out.println("Total objects created: " + resourcePool.getTotalCreatedObjects());
        System.out.println("Leased objects: " + resourcePool.getLeasedObjectsCount());
        
        // Cleanup
        resourcePool.shutdown();
    }
}