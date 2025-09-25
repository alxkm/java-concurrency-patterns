package org.alxkm.patterns.leaderfollower;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;

/**
 * Leader-Follower Pattern implementation.
 * 
 * This pattern efficiently manages a pool of threads where one thread acts as the 
 * leader (waiting for events), while others act as followers (waiting to become leader).
 * When an event arrives, the leader processes it and promotes a follower to become 
 * the new leader, then the original leader becomes a follower again.
 * 
 * This pattern is useful for server applications where you want to minimize thread 
 * switching overhead and ensure only one thread is actively waiting for events.
 */
public class LeaderFollowerPattern<T> {
    
    private final BlockingQueue<T> eventQueue;
    private final Consumer<T> eventProcessor;
    private final int threadPoolSize;
    private final AtomicBoolean isRunning;
    private final AtomicInteger activeThreads;
    
    // Leader-Follower synchronization
    private final ReentrantLock leaderLock;
    private final Condition followerCondition;
    private volatile Thread currentLeader;
    private final BlockingQueue<Thread> followerQueue;
    
    // Statistics
    private final AtomicInteger processedEvents;
    private final AtomicInteger leaderPromotions;
    
    /**
     * Creates a new Leader-Follower thread pool.
     * 
     * @param threadPoolSize Number of threads in the pool
     * @param eventProcessor Function to process incoming events
     */
    public LeaderFollowerPattern(int threadPoolSize, Consumer<T> eventProcessor) {
        if (threadPoolSize <= 0) {
            throw new IllegalArgumentException("Thread pool size must be positive");
        }
        if (eventProcessor == null) {
            throw new IllegalArgumentException("Event processor cannot be null");
        }
        
        this.threadPoolSize = threadPoolSize;
        this.eventProcessor = eventProcessor;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.isRunning = new AtomicBoolean(false);
        this.activeThreads = new AtomicInteger(0);
        
        this.leaderLock = new ReentrantLock();
        this.followerCondition = leaderLock.newCondition();
        this.currentLeader = null;
        this.followerQueue = new LinkedBlockingQueue<>();
        
        this.processedEvents = new AtomicInteger(0);
        this.leaderPromotions = new AtomicInteger(0);
    }
    
    /**
     * Worker thread that implements the Leader-Follower pattern.
     */
    private class WorkerThread extends Thread {
        private final int threadId;
        
        public WorkerThread(int threadId) {
            this.threadId = threadId;
            setName("LeaderFollower-Worker-" + threadId);
        }
        
        @Override
        public void run() {
            activeThreads.incrementAndGet();
            
            try {
                while (isRunning.get()) {
                    if (becomeLeader()) {
                        // I am the leader, wait for and process events
                        processAsLeader();
                    } else {
                        // I am a follower, wait to be promoted
                        waitAsFollower();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeThreads.decrementAndGet();
            }
        }
        
        /**
         * Attempt to become the leader.
         */
        private boolean becomeLeader() {
            leaderLock.lock();
            try {
                if (currentLeader == null) {
                    currentLeader = Thread.currentThread();
                    leaderPromotions.incrementAndGet();
                    return true;
                } else {
                    // Add self to follower queue
                    followerQueue.offer(Thread.currentThread());
                    return false;
                }
            } finally {
                leaderLock.unlock();
            }
        }
        
        /**
         * Process events as the leader.
         */
        private void processAsLeader() throws InterruptedException {
            try {
                while (isRunning.get() && currentLeader == Thread.currentThread()) {
                    T event = eventQueue.poll(); // Non-blocking poll
                    
                    if (event != null) {
                        // Process the event
                        try {
                            eventProcessor.accept(event);
                            processedEvents.incrementAndGet();
                        } catch (Exception e) {
                            System.err.println("Error processing event: " + e.getMessage());
                        }
                        
                        // After processing, promote a follower to leader and step down
                        promoteFollowerToLeader();
                        break; // Exit leader role
                        
                    } else {
                        // No event available, brief wait
                        Thread.sleep(1);
                    }
                }
            } finally {
                // Ensure we step down as leader if we're still the leader
                stepDownAsLeader();
            }
        }
        
        /**
         * Wait as a follower to be promoted to leader.
         */
        private void waitAsFollower() throws InterruptedException {
            leaderLock.lock();
            try {
                while (isRunning.get() && currentLeader != Thread.currentThread()) {
                    followerCondition.await();
                }
            } finally {
                leaderLock.unlock();
            }
        }
        
        /**
         * Promote a follower to become the new leader.
         */
        private void promoteFollowerToLeader() {
            leaderLock.lock();
            try {
                Thread nextLeader = followerQueue.poll();
                if (nextLeader != null) {
                    currentLeader = nextLeader;
                    leaderPromotions.incrementAndGet();
                    followerCondition.signalAll(); // Wake up the new leader
                } else {
                    currentLeader = null; // No followers available
                }
            } finally {
                leaderLock.unlock();
            }
        }
        
        /**
         * Step down as leader.
         */
        private void stepDownAsLeader() {
            leaderLock.lock();
            try {
                if (currentLeader == Thread.currentThread()) {
                    currentLeader = null;
                }
            } finally {
                leaderLock.unlock();
            }
        }
    }
    
    /**
     * Starts the Leader-Follower thread pool.
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            // Start worker threads
            for (int i = 0; i < threadPoolSize; i++) {
                new WorkerThread(i).start();
            }
            
            // Wait for threads to initialize
            while (activeThreads.get() < threadPoolSize) {
                Thread.yield();
            }
        }
    }
    
    /**
     * Submits an event for processing.
     * 
     * @param event The event to process
     * @return true if the event was accepted, false otherwise
     */
    public boolean submitEvent(T event) {
        if (!isRunning.get()) {
            return false;
        }
        return eventQueue.offer(event);
    }
    
    /**
     * Submits an event and blocks until it's accepted.
     * 
     * @param event The event to process
     * @throws InterruptedException if interrupted while waiting
     */
    public void submitEventBlocking(T event) throws InterruptedException {
        if (!isRunning.get()) {
            throw new IllegalStateException("Thread pool is not running");
        }
        eventQueue.put(event);
    }
    
    /**
     * Shuts down the thread pool gracefully.
     */
    public void shutdown() {
        isRunning.set(false);
        
        // Wake up all waiting followers
        leaderLock.lock();
        try {
            followerCondition.signalAll();
        } finally {
            leaderLock.unlock();
        }
        
        // Wait for all threads to finish
        long timeout = System.currentTimeMillis() + 5000; // 5 second timeout
        while (activeThreads.get() > 0 && System.currentTimeMillis() < timeout) {
            Thread.yield();
        }
    }
    
    /**
     * Forces immediate shutdown.
     */
    public void shutdownNow() {
        isRunning.set(false);
        
        // Clear queues
        eventQueue.clear();
        followerQueue.clear();
        
        // Wake up all waiting threads
        leaderLock.lock();
        try {
            followerCondition.signalAll();
        } finally {
            leaderLock.unlock();
        }
    }
    
    /**
     * Checks if the thread pool is running.
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * Gets the number of active threads.
     */
    public int getActiveThreadCount() {
        return activeThreads.get();
    }
    
    /**
     * Gets the number of pending events.
     */
    public int getPendingEventCount() {
        return eventQueue.size();
    }
    
    /**
     * Gets the number of events processed.
     */
    public int getProcessedEventCount() {
        return processedEvents.get();
    }
    
    /**
     * Gets the number of leader promotions.
     */
    public int getLeaderPromotionCount() {
        return leaderPromotions.get();
    }
    
    /**
     * Gets the current leader thread (for testing purposes).
     */
    public Thread getCurrentLeader() {
        return currentLeader;
    }
    
    /**
     * Example event class.
     */
    public static class Event {
        private final int id;
        private final String data;
        private final long timestamp;
        
        public Event(int id, String data) {
            this.id = id;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getId() {
            return id;
        }
        
        public String getData() {
            return data;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "Event{id=" + id + ", data='" + data + "', timestamp=" + timestamp + "}";
        }
    }
    
    /**
     * Example usage demonstrating the Leader-Follower pattern.
     */
    public static void main(String[] args) throws InterruptedException {
        // Create a Leader-Follower thread pool
        LeaderFollowerPattern<Event> leaderFollower = new LeaderFollowerPattern<>(
            4, // 4 threads
            event -> {
                // Event processor
                System.out.println("Processing " + event + " on thread " + 
                                 Thread.currentThread().getName());
                try {
                    // Simulate processing time
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        );
        
        System.out.println("Starting Leader-Follower pattern with " + 
                         leaderFollower.threadPoolSize + " threads");
        
        // Start the thread pool
        leaderFollower.start();
        
        // Submit events from multiple producer threads
        Thread[] producers = new Thread[3];
        for (int i = 0; i < producers.length; i++) {
            final int producerId = i;
            producers[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    Event event = new Event(
                        producerId * 100 + j,
                        "Data from producer " + producerId + ", event " + j
                    );
                    
                    if (leaderFollower.submitEvent(event)) {
                        System.out.println("Producer " + producerId + " submitted: " + event.getId());
                    } else {
                        System.out.println("Producer " + producerId + " failed to submit: " + event.getId());
                    }
                    
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            producers[i].start();
        }
        
        // Wait for producers to finish
        for (Thread producer : producers) {
            producer.join();
        }
        
        // Let processing finish
        Thread.sleep(2000);
        
        // Print statistics
        System.out.println("\nFinal Statistics:");
        System.out.println("Active threads: " + leaderFollower.getActiveThreadCount());
        System.out.println("Processed events: " + leaderFollower.getProcessedEventCount());
        System.out.println("Leader promotions: " + leaderFollower.getLeaderPromotionCount());
        System.out.println("Pending events: " + leaderFollower.getPendingEventCount());
        
        // Shutdown
        System.out.println("\nShutting down...");
        leaderFollower.shutdown();
        System.out.println("Shutdown complete");
    }
}