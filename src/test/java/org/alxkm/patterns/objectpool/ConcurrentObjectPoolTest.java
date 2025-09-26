package org.alxkm.patterns.objectpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentObjectPoolTest {

    private ConcurrentObjectPool<ConcurrentObjectPool.PoolableResource> pool;
    private AtomicInteger resourceIdGenerator;

    @BeforeEach
    public void setUp() {
        resourceIdGenerator = new AtomicInteger(0);
        pool = new ConcurrentObjectPool<>(
            () -> new ConcurrentObjectPool.PoolableResource(resourceIdGenerator.incrementAndGet()),
            new ConcurrentObjectPool.ResourceValidator(),
            5
        );
    }

    @AfterEach
    public void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Test
    public void testBasicAcquireAndRelease() {
        ConcurrentObjectPool.PoolableResource resource = pool.acquire();
        
        assertNotNull(resource);
        assertEquals(1, pool.getLeasedObjectsCount());
        assertTrue(pool.getCurrentPoolSize() >= 0);
        
        pool.release(resource);
        assertEquals(0, pool.getLeasedObjectsCount());
    }

    @Test
    public void testPoolInitialization() {
        assertTrue(pool.getCurrentPoolSize() > 0, "Pool should be pre-populated");
        assertEquals(5, pool.getMaxPoolSize());
        assertEquals(0, pool.getLeasedObjectsCount());
    }

    @Test
    public void testAcquireWithTimeout() {
        // Fill up the pool
        ConcurrentObjectPool.PoolableResource[] resources = 
            new ConcurrentObjectPool.PoolableResource[pool.getMaxPoolSize()];
        
        for (int i = 0; i < resources.length; i++) {
            resources[i] = pool.acquire();
            assertNotNull(resources[i]);
        }

        // Try to acquire with timeout when pool is exhausted
        long startTime = System.currentTimeMillis();
        ConcurrentObjectPool.PoolableResource resource = pool.acquire(100);
        long duration = System.currentTimeMillis() - startTime;

        assertNull(resource);
        assertTrue(duration >= 100, "Should respect timeout");

        // Release one resource and try again
        pool.release(resources[0]);
        resource = pool.acquire(100);
        assertNotNull(resource);

        // Clean up
        for (int i = 1; i < resources.length; i++) {
            pool.release(resources[i]);
        }
        pool.release(resource);
    }

    @Test
    public void testConcurrentAcquireRelease() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 20;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        ConcurrentObjectPool.PoolableResource resource = pool.acquire();
                        if (resource != null) {
                            resource.use();
                            
                            // Simulate some work
                            Thread.sleep(1);
                            
                            pool.release(resource);
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        
        int totalOperations = threadCount * operationsPerThread;
        assertEquals(totalOperations, successCount.get() + failureCount.get());
        assertTrue(successCount.get() > 0, "At least some operations should succeed");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testObjectValidation() {
        // Create pool with custom validator that rejects objects with even IDs
        ConcurrentObjectPool<ConcurrentObjectPool.PoolableResource> customPool = 
            new ConcurrentObjectPool<>(
                () -> new ConcurrentObjectPool.PoolableResource(resourceIdGenerator.incrementAndGet()),
                new ConcurrentObjectPool.ObjectValidator<ConcurrentObjectPool.PoolableResource>() {
                    @Override
                    public boolean isValid(ConcurrentObjectPool.PoolableResource resource) {
                        return resource != null && resource.getId() % 2 != 0; // Only odd IDs are valid
                    }
                    
                    @Override
                    public void reset(ConcurrentObjectPool.PoolableResource resource) {
                        if (resource != null) {
                            resource.release();
                        }
                    }
                },
                3
            );

        ConcurrentObjectPool.PoolableResource resource = customPool.acquire();
        assertNotNull(resource);
        assertTrue(resource.getId() % 2 != 0, "Should only get odd-numbered resources");
        
        customPool.release(resource);
        customPool.shutdown();
    }

    //@Test
    public void testPoolExhaustion() {
        ConcurrentObjectPool<String> stringPool = new ConcurrentObjectPool<>(
            () -> "TestString",
            2 // Small pool size
        );

        String obj1 = stringPool.acquire();
        String obj2 = stringPool.acquire();
        String obj3 = stringPool.acquire(); // Should still work, creates new object

        assertNotNull(obj1);
        assertNotNull(obj2);
        assertNotNull(obj3);

        assertEquals(3, stringPool.getLeasedObjectsCount());

        stringPool.release(obj1);
        stringPool.release(obj2);
        stringPool.release(obj3);

        assertEquals(0, stringPool.getLeasedObjectsCount());
        stringPool.shutdown();
    }

    @Test
    public void testInvalidObjectHandling() {
        ConcurrentObjectPool.PoolableResource resource = pool.acquire();
        assertNotNull(resource);

        // Release the same object twice (second release should be ignored)
        pool.release(resource);
        assertEquals(0, pool.getLeasedObjectsCount());

        pool.release(resource); // Should be ignored
        assertEquals(0, pool.getLeasedObjectsCount());
    }

    @Test
    public void testNullObjectHandling() {
        pool.release(null); // Should not cause any issues
        assertEquals(0, pool.getLeasedObjectsCount());
    }

    @Test
    public void testStatistics() {
        int initialCreatedCount = pool.getTotalCreatedObjects();
        assertTrue(initialCreatedCount > 0, "Should have created initial objects");

        ConcurrentObjectPool.PoolableResource resource1 = pool.acquire();
        ConcurrentObjectPool.PoolableResource resource2 = pool.acquire();

        assertEquals(2, pool.getLeasedObjectsCount());
        assertTrue(pool.getTotalCreatedObjects() >= initialCreatedCount);

        pool.release(resource1);
        assertEquals(1, pool.getLeasedObjectsCount());

        pool.release(resource2);
        assertEquals(0, pool.getLeasedObjectsCount());
    }

    @Test
    public void testResourceUsage() {
        ConcurrentObjectPool.PoolableResource resource = pool.acquire();
        assertNotNull(resource);
        assertFalse(resource.isInUse());

        resource.use();
        assertTrue(resource.isInUse());

        pool.release(resource);
        assertFalse(resource.isInUse()); // Should be reset by validator
    }

    @Test
    public void testConcurrentPoolOperations() throws InterruptedException {
        final int threadCount = 20;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < 10; j++) {
                        ConcurrentObjectPool.PoolableResource resource = pool.acquire();
                        if (resource != null) {
                            Thread.sleep(1); // Brief work simulation
                            pool.release(resource);
                        }
                    }
                } catch (Exception e) {
                    exceptionRef.set(e);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        assertNull(exceptionRef.get(), "No exceptions should occur during concurrent operations");
    }

    @Test
    public void testPoolShutdown() {
        assertTrue(pool.getCurrentPoolSize() > 0);
        
        pool.shutdown();
        assertEquals(0, pool.getCurrentPoolSize());
        assertEquals(0, pool.getLeasedObjectsCount());
    }

    @Test
    public void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConcurrentObjectPool<>(null, 5);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ConcurrentObjectPool<>(() -> "test", 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ConcurrentObjectPool<>(() -> "test", -1);
        });
    }

    @Test
    public void testLongRunningStressTest() throws InterruptedException {
        final int threadCount = 8;
        final int duration = 1000; // 1 second
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger operationCount = new AtomicInteger(0);
        final long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    while (System.currentTimeMillis() - startTime < duration) {
                        ConcurrentObjectPool.PoolableResource resource = pool.acquire();
                        if (resource != null) {
                            operationCount.incrementAndGet();
                            Thread.sleep(1);
                            pool.release(resource);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(duration + 1000, TimeUnit.MILLISECONDS));
        assertTrue(operationCount.get() > 0, "Should perform operations during stress test");
        assertEquals(0, pool.getLeasedObjectsCount(), "All resources should be returned");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }
}