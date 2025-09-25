package org.alxkm.patterns.builder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadSafeBuilderTest {

    @Test
    public void testBasicBuilderFunctionality() {
        ThreadSafeBuilder.Product product = ThreadSafeBuilder.newBuilder()
                .setName("Test Product")
                .setValue(42)
                .setEnabled(true)
                .setDescription("A test product")
                .build();

        assertEquals("Test Product", product.getName());
        assertEquals(42, product.getValue());
        assertTrue(product.isEnabled());
        assertEquals("A test product", product.getDescription());
    }

    @Test
    public void testBuilderCanOnlyBeUsedOnce() {
        ThreadSafeBuilder.Builder builder = ThreadSafeBuilder.newBuilder()
                .setName("First Product")
                .setValue(100);

        ThreadSafeBuilder.Product product = builder.build();
        assertNotNull(product);
        assertEquals("First Product", product.getName());
        assertEquals(100, product.getValue());

        assertThrows(IllegalStateException.class, () -> {
            builder.setName("Second Product");
        });

        assertThrows(IllegalStateException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void testConcurrentBuilding() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    ThreadSafeBuilder.Builder builder = ThreadSafeBuilder.newBuilder();
                    ThreadSafeBuilder.Product product = builder
                            .setName("Product-" + threadId)
                            .setValue(threadId * 10)
                            .setEnabled(threadId % 2 == 0)
                            .setDescription("Product from thread " + threadId)
                            .build();

                    assertNotNull(product);
                    assertEquals("Product-" + threadId, product.getName());
                    assertEquals(threadId * 10, product.getValue());
                    assertEquals(threadId % 2 == 0, product.isEnabled());
                    assertEquals("Product from thread " + threadId, product.getDescription());
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount, successCount.get());
        assertEquals(0, errorCount.get());
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testBuilderThreadSafety() throws InterruptedException {
        ThreadSafeBuilder.Builder builder = ThreadSafeBuilder.newBuilder();
        final int threadCount = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    builder.setName("Name-" + threadId);
                    builder.setValue(threadId);
                    builder.setEnabled(true);
                    builder.setDescription("Description-" + threadId);
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected when builder is already built
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        
        // Only one thread should be able to build successfully
        ThreadSafeBuilder.Product product = builder.build();
        assertNotNull(product);
        
        // At least one thread should have succeeded in setting values
        assertTrue(successCount.get() >= 1);
    }

    @Test
    public void testPartialBuilding() {
        ThreadSafeBuilder.Product product = ThreadSafeBuilder.newBuilder()
                .setName("Partial Product")
                .build();

        assertEquals("Partial Product", product.getName());
        assertEquals(0, product.getValue()); // default value
        assertFalse(product.isEnabled()); // default value
        assertNull(product.getDescription()); // default value
    }

    @Test
    public void testBuilderChaining() {
        ThreadSafeBuilder.Builder builder = ThreadSafeBuilder.newBuilder();
        
        ThreadSafeBuilder.Product product = builder
                .setName("Chained")
                .setValue(999)
                .setEnabled(false)
                .setDescription("Chained building")
                .build();

        assertEquals("Chained", product.getName());
        assertEquals(999, product.getValue());
        assertFalse(product.isEnabled());
        assertEquals("Chained building", product.getDescription());
    }

    @Test
    public void testMultipleBuilders() throws InterruptedException {
        final int builderCount = 20;
        final CountDownLatch latch = new CountDownLatch(builderCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < builderCount; i++) {
            final int builderId = i;
            executor.submit(() -> {
                try {
                    ThreadSafeBuilder.Product product = ThreadSafeBuilder.newBuilder()
                            .setName("Builder-" + builderId)
                            .setValue(builderId)
                            .setEnabled(builderId % 3 == 0)
                            .setDescription("From builder " + builderId)
                            .build();

                    assertNotNull(product);
                    assertEquals("Builder-" + builderId, product.getName());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(builderCount, successCount.get());
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }
}