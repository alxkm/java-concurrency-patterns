package org.alxkm.patterns.queue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The CustomBlockingQueue class represents a blocking queue with a fixed capacity. It provides methods to enqueue and dequeue
 * elements from the queue safely in a multi-threaded environment.
 *
 * @param <E> The type of elements stored in the queue.
 */
public class CustomBlockingQueue<E> {
    private final Queue<E> queue;
    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    /**
     * Constructs a CustomBlockingQueue with the specified capacity.
     *
     * @param capacity The maximum number of elements the queue can hold.
     */
    public CustomBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
    }

    /**
     * Inserts the specified element into this queue, waiting if necessary for space to become available.
     *
     * @param element The element to be inserted.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public void enqueue(E element) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.offer(element);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
     *
     * @return The element at the head of the queue.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public E dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            E element = queue.poll();
            notFull.signal();
            return element;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of elements in this queue.
     *
     * @return The number of elements in this queue.
     */
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
}