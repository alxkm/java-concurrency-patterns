# Java multithreading and concurrency patterns and antipatterns
[![Java CI with Gradle](https://github.com/alxkm/java-concurrency-patterns/actions/workflows/gradle.yml/badge.svg)](https://github.com/alxkm/java-concurrency-patterns/actions/workflows/gradle.yml)[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Patterns:

### Atomics examples:
- [AtomicExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicExample.java): Demonstrates basic atomic operations.
- [MyClass.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/MyClass.java): Example class used in atomic operations.
- [AtomicIntegerFieldUpdaterExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicIntegerFieldUpdaterExample.java): Demonstrates atomic field updater for integer fields.
- [AtomicLongFieldUpdaterExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicLongFieldUpdaterExample.java): Demonstrates atomic field updater for long fields.
- [SharedResource.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/SharedResource.java): Example class representing a shared resource.
- [AtomicMarkableReferenceExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicMarkableReferenceExample.java): Example of AtomicMarkableReference usage.
- [AtomicReferenceArrayExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicReferenceArrayExample.java): Demonstrates atomic operations on arrays.
- [AtomicReferenceExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicReferenceExample.java): Example of using AtomicReference.
- [AtomicReferenceFieldUpdaterExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicReferenceFieldUpdaterExample.java): Demonstrates atomic field updater for reference fields.
- [AtomicStampedReferenceExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicStampedReferenceExample.java): Example of using AtomicStampedReference.

### Concurrent collections
- [ConcurrentHashMapExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/collections/ConcurrentHashMapExample.java): Demonstrates usage of ConcurrentHashMap.
- [ConcurrentSkipListMapExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/collections/ConcurrentSkipListMapExample.java): Example of using ConcurrentSkipListMap.
- [ConcurrentSkipListSetExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/collections/ConcurrentSkipListSetExample.java): Demonstrates usage of ConcurrentSkipListSet.
- [CopyOnWriteArrayListExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/collections/CopyOnWriteArrayListExample.java): Example of CopyOnWriteArrayList usage.

### Queue
- [ArrayBlockingQueueExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/queue/ArrayBlockingQueueExample.java): Example of ArrayBlockingQueue usage.
- [ConcurrentLinkedDequeExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/queue/ConcurrentLinkedDequeExample.java): Demonstrates usage of ConcurrentLinkedDeque.
- [ConcurrentLinkedQueueExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/queue/ConcurrentLinkedQueueExample.java): Example of ConcurrentLinkedQueue usage.
- [Consumer.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/queue/Consumer.java): Represents a consumer in producer-consumer problem.
- [CustomBlockingQueue.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/queue/CustomBlockingQueue.java): Example of a custom blocking queue.
- [Producer.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/queue/Producer.java): Represents a producer in producer-consumer problem.
- [ProducerConsumerBlockingQueueExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/queue/ProducerConsumerBlockingQueueExample.java): Demonstrates producer-consumer problem using blocking queue.
- [BlockingQueueSimpleExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/collections/BlockingQueueSimpleExample.java): Simple example of a blocking queue.

### Executors example
- [AbstractExecutorServiceExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/executors/AbstractExecutorServiceExample.java): Example of AbstractExecutorService usage.
- [CompletionServiceExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/executors/CompletionServiceExample.java): Demonstrates usage of CompletionService.
- [ExecutorServiceExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/executors/ExecutorServiceExample.java): Example of using ExecutorService.
- [ExecutorsExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/executors/ExecutorsExample.java): Demonstrates various executor services.
- [ScheduledThreadPoolExecutorExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/executors/ScheduledThreadPoolExecutorExample.java): Example of ScheduledThreadPoolExecutor usage.
- [Task.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/executors/Task.java): Represents a task for executor services.
- [ThreadPoolExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/executors/ThreadPoolExample.java): Demonstrates thread pool usage.
- [ThreadPoolExecutorExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/executors/ThreadPoolExecutorExample.java): Example of ThreadPoolExecutor usage.

### Fork join pool example
- [MergeSortTask.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/forkjoinpool/MergeSortTask.java): Task for merge sort using ForkJoinPool.
- [ForkJoinMergeSort.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/forkjoinpool/ForkJoinMergeSort.java): Example of merge sort using ForkJoinPool.
- [ForkJoinPoolExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/forkjoinpool/ForkJoinPoolExample.java): Demonstrates ForkJoinPool usage.
- [MyRecursiveTask.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/forkjoinpool/MyRecursiveTask.java): Custom recursive task example.

### Future example
- [FutureExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/future/FutureExample.java): Demonstrates usage of Future.
- [Task.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/future/Task.java): Represents a task for future computation.

### Locks
- [CustomSynchronizer.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/CustomSynchronizer.java): Example of a custom synchronizer.
- [AbstractOwnableSynchronizerExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/AbstractOwnableSynchronizerExample.java): Demonstrates AbstractOwnableSynchronizer usage.
- [AbstractQueuedLongSynchronizerExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/AbstractQueuedLongSynchronizerExample.java): Example of AbstractQueuedLongSynchronizer usage.
- [AbstractQueuedSynchronizerExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/AbstractQueuedSynchronizerExample.java): Demonstrates AbstractQueuedSynchronizer usage.
- [Task.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/Task.java): Represents a task using locks.
- [LockSupportExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/LockSupportExample.java): Demonstrates usage of LockSupport.
- [ReadWriteLockExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/ReadWriteLockExample.java): Example of ReadWriteLock usage.
- [ReentrantReadWriteLockCounter.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/ReentrantReadWriteLockCounter.java): Counter using ReentrantReadWriteLock.
- [ReentrantReadWriteLockCounterExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/locks/ReentrantReadWriteLockCounterExample.java): Demonstrates counter with ReentrantReadWriteLock.

### Synchronizers
- [Barrier.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/Barrier.java): Demonstrates a custom barrier implementation.
- [BarrierExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/BarrierExample.java): Example of using barriers.
- [CountDownLatchExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/CountDownLatchExample.java): Demonstrates usage of CountDownLatch.
- [CustomTask.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/CustomTask.java): Represents a custom task using synchronizers.
- [Consumer.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/Consumer.java): Consumer in a synchronization context.
- [Producer.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/Producer.java): Producer in a synchronization context.
- [ExchangerExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/ExchangerExample.java): Demonstrates usage of Exchanger.
- [PhaserExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/PhaserExample.java): Example of using Phaser.
- [SemaphorePrintQueueExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/synchronizers/SemaphorePrintQueueExample.java): Demonstrates a print queue using semaphore.

### Thread local
- [ThreadLocalExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/threadlocal/ThreadLocalExample.java): Example of using ThreadLocal.

### Reentrant lock
- [ReentrantLockCounter.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/reentrantlock/ReentrantLockCounter.java): Counter using ReentrantLock.
- [ReentrantLockExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/reentrantlock/ReentrantLockExample.java): Demonstrates usage of ReentrantLock.

### Mutex
- [MutexExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/mutex/MutexExample.java): Demonstrates usage of a mutex.

### Semaphore
- [SemaphoreExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/semaphore/SemaphoreExample.java): Demonstrates usage of a semaphore.

### Double check locking singleton
- [DoubleCheckedLockingSingleton.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/doublechecklocking/DoubleCheckedLockingSingleton.java): Demonstrates double-checked locking for singleton pattern.
 
### Active object:
- [ActiveObject.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/activeobject/ActiveObject.java): Example of Active Object pattern.

### Balking pattern
- [BalkingPatternExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/balking/BalkingPatternExample.java): Demonstrates the Balking pattern.

### Guarded suspension pattern
- [GuardedSuspensionExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/guardedsuspension/GuardedSuspensionExample.java): Demonstrates the Guarded Suspension pattern.

### Immutable pattern
- [Immutable.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/immutable/Immutable.java): Example of an immutable object.

### Monitor object pattern
- [MonitorObject.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/monitorobject/MonitorObject.java): Example of the monitor object pattern.

### Multithreaded context
- [MultithreadedContext.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/multithreadedcontext/MultithreadedContext.java): Example of multithreaded context.

### Reactor pattern
- [Reactor.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/reactor/Reactor.java): Example of the reactor pattern.

### Scheduler
- [Scheduler.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/scheduler/Scheduler.java): Example of a task scheduler.

### Singleton pattern
- [Singleton.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/singleton/Singleton.java): Example of the singleton pattern.

### Thread-safe lazy initialization
- [Holder.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/threadsafelazyinitialization/Holder.java): Demonstrates thread-safe lazy initialization using holder pattern.
- [LazyInitialization.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/threadsafelazyinitialization/LazyInitialization.java): Example of thread-safe lazy initialization.

### Two-phase termination
- [TwoPhaseTermination.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/twophasetermination/TwoPhaseTermination.java): Demonstrates the two-phase termination pattern.

### Odd-Even Printer
- [OddEvenPrinter.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/oddevenprinter/OddEvenPrinter.java): Example of odd-even printing using threads.
- [OddEvenPrinterExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/oddevenprinter/OddEvenPrinterExample.java): Demonstrates odd-even printer example.

### Philosopher problem
- [PhilosopherWithLock.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/philosopher/PhilosopherWithLock.java): Philosopher problem using locks.
- [PhilosopherWithSemaphore.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/patterns/philosopher/PhilosopherWithSemaphore.java): Philosopher problem using semaphores.


# Antipatterns

### Thread Leakage

- Description: Threads are created but never terminated, leading to resource exhaustion.
- Solution: Use thread pools (e.g., ThreadPoolExecutor) to manage threads.

#### Examples:
- [ThreadLeakageExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/threadleakage/ThreadLeakageExample.java)
- [ThreadLeakageResolution.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/threadleakage/ThreadLeakageResolution.java)

### Busy Waiting

- Description: A thread repeatedly checks a condition in a loop, wasting CPU cycles.
- Solution: Use wait/notify mechanisms or higher-level concurrency constructs like CountDownLatch, CyclicBarrier, or Condition.

#### Examples:
- [BusyWaitingExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/busywaiting/BusyWaitingExample.java)
- [BusyWaitingResolution.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/busywaiting/BusyWaitingResolution.java)

### Nested Monitor Lockout (Deadlock)

- Description: Two or more threads block each other by holding resources the other needs.
- Solution: Always acquire multiple locks in a consistent global order, use tryLock with timeouts, or avoid acquiring multiple locks if possible.

#### Examples:
- [DeadlockExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/deadlock/DeadlockExample.java)
- [DeadlockResolutionExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/deadlock/DeadlockResolutionExample.java)

### Forgotten Synchronization

- Description: Access to shared resources is not properly synchronized, leading to race conditions.
- Solution: Use synchronized blocks or higher-level concurrency utilities (e.g., ReentrantLock, Atomic* classes).

#### Examples:
- [CounterExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/forgottensynchronization/CounterExample.java)
- [CounterReentrantLockResolution.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/forgottensynchronization/CounterReentrantLockResolution.java)
- [CounterResolution.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/forgottensynchronization/CounterResolution.java)

### Excessive Synchronization

- Description: Overuse of synchronization, leading to contention and reduced parallelism.
- Solution: Minimize the scope of synchronized blocks, use lock-free algorithms, or utilize concurrent collections (e.g., ConcurrentHashMap, CopyOnWriteArrayList).

#### Examples:
- [AtomicCounter.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/excessivesynchronization/AtomicCounter.java)
- [ExcessiveSyncCounter.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/excessivesynchronization/ExcessiveSyncCounter.java)
- [OptimizedCounter.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/excessivesynchronization/OptimizedCounter.java)

### Using Thread-Safe Collections Incorrectly

- Description: Assuming individual thread-safe operations guarantee overall thread-safe logic.
- Solution: Combine operations using explicit locks or use higher-level synchronization constructs to maintain logical thread safety.

#### Examples:
- [CorrectUsage.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/usingthreadsafecollectionsincorrectly/CorrectUsage.java)
- [IncorrectUsage.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/usingthreadsafecollectionsincorrectly/IncorrectUsage.java)
- [OptimizedUsage.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/usingthreadsafecollectionsincorrectly/OptimizedUsage.java)

### Ignoring InterruptedException

- Description: Swallowing or ignoring the InterruptedException, leading to threads that cannot be properly managed or interrupted.
- Solution: Handle interruptions properly, typically by cleaning up and propagating the interruption status.

#### Examples:
- [IgnoringInterruptedException.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/ignoringinterruptedexception/IgnoringInterruptedException.java)
- [PropagatingInterruptedException.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/ignoringinterruptedexception/PropagatingInterruptedException.java)
- [ProperlyHandlingInterruptedException.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/ignoringinterruptedexception/ProperlyHandlingInterruptedException.java)

### Starting a Thread in a Constructor

- Description: Starting a thread from within a constructor, possibly before the object is fully constructed.
- Solution: Start threads from a dedicated method called after construction, or use factory methods.

#### Examples:
- [ThreadInConstructor.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/threadinconstructor/ThreadInConstructor.java)
- [ThreadStartedOutsideConstructor.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/threadinconstructor/ThreadStartedOutsideConstructor.java)
- [ThreadUsingFactory.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/threadinconstructor/ThreadUsingFactory.java)

### Double-Checked Locking

- Description: A broken idiom for lazy initialization that was incorrectly implemented before Java 5.
- Solution: Use the volatile keyword correctly or the Initialization-on-demand holder idiom.

#### Examples:
- [Singleton.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/doublechecklocking/Singleton.java)
- [Holder.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/doublechecklocking/Holder.java)
- [SingletonInitializationOnDemand.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/doublechecklocking/SingletonInitializationOnDemand.java)
- [SingletonWithVolatile.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/doublechecklocking/SingletonWithVolatile.java)

### Lock Contention

- Description: Multiple threads trying to acquire the same lock, leading to reduced performance.
- Solution: Reduce the granularity of locks, use read-write locks, or employ lock-free data structures.

#### Examples:
- [LockContentionExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/lockcontention/LockContentionExample.java)
- [LockContentionResolution.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/lockcontention/LockContentionResolution.java)
- [StampedLockExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/lockcontention/StampedLockExample.java)

### Improper Use of ThreadLocal

- Description: Using ThreadLocal incorrectly, leading to memory leaks or unexpected behavior.
- Solution: Ensure proper management and cleanup of ThreadLocal variables.

#### Examples:
- [ThreadLocalCleanupExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/improperuseofthreadlocal/ThreadLocalCleanupExample.java)
- [ThreadLocalExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/improperuseofthreadlocal/ThreadLocalExample.java)
- [ThreadLocalCleaner.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/improperuseofthreadlocal/ThreadLocalCleaner.java)
- [ThreadLocalWithResourceExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/improperuseofthreadlocal/ThreadLocalWithResourceExample.java)

### Non-Atomic Compound Actions

- Description: Performing compound actions (e.g., check-then-act, read-modify-write) without proper synchronization.
- Solution: Use atomic variables or synchronized blocks to ensure compound actions are atomic.

#### Examples:
- [AtomicCompoundActionsExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/nonatomiccompoundactions/AtomicCompoundActionsExample.java)
- [AtomicIntegerExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/nonatomiccompoundactions/AtomicIntegerExample.java)
- [NonAtomicCompoundActionsExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/nonatomiccompoundactions/NonAtomicCompoundActionsExample.java)

### Race Conditions

- Description: The system's behavior depends on the sequence or timing of uncontrollable events.
- Solution: Properly synchronize access to shared resources and use thread-safe collections.

#### Examples:
- [AccountAmount.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/racecondition/AccountAmount.java)
- [AccountExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/racecondition/AccountExample.java)

### Lack of Thread Safety in Singletons

- Description: Singleton instances not properly synchronized, leading to multiple instances.
- Solution: Use the enum singleton pattern or the Initialization-on-demand holder idiom.

#### Examples:
- [DoubleCheckedLockingSingleton.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/lackofthreadsafetyinsingletons/DoubleCheckedLockingSingleton.java)
- [Holder.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/lackofthreadsafetyinsingletons/Holder.java)
- [HolderSingleton.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/lackofthreadsafetyinsingletons/HolderSingleton.java)
- [SafeSingleton.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/lackofthreadsafetyinsingletons/SafeSingleton.java)
- [UnsafeSingleton.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/lackofthreadsafetyinsingletons/UnsafeSingleton.java)

### Using Threads Instead of Tasks

- Description: Directly creating and managing threads instead of using the Executor framework.
- Solution: Use ExecutorService and related classes to manage thread pools and tasks efficiently.

#### Examples:
- [CompletableFutureExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/threadsinsteadoftasks/CompletableFutureExample.java)
- [DirectThreadManagement.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/threadsinsteadoftasks/DirectThreadManagement.java)
- [ExecutorFrameworkExample.java](https://github.com/alxkm/java-concurrency/blob/main/src/main/java/org/alxkm/antipatterns/threadsinsteadoftasks/ExecutorFrameworkExample.java)



# java.util.concurrent.*

![image](images/structure.jpeg)


**Concurrent Collections** are a set of collections designed to operate more efficiently in multithreaded environments compared to the standard universal collections from the java.util package. Instead of using the basic Collections.synchronizedList wrapper, which blocks access to the entire collection, these collections utilize locks on data segments or employ wait-free algorithms to optimize parallel data reading and processing.

**Queues** — non-blocking and blocking queues with multithreading support. Non-blocking queues are designed for speed and work without blocking threads. Blocking queues are used when it is necessary to "slow down" the "Producer" or "Consumer" threads if some conditions are not met, for example, the queue is empty or full, or there is no free "Consumer".

**Synchronizers** are auxiliary utilities for synchronizing threads. They are a powerful weapon in "parallel" computing.

**Executors** - contains excellent frameworks for creating thread pools, scheduling asynchronous tasks and obtaining results.

**Locks** are alternative and more flexible thread synchronization mechanisms compared to the basic synchronized, wait, notify, notifyAll.

**Atomics** — classes with support for atomic operations on primitives and references.

# Concurrent Collections

### CopyOnWrite collections

![image](images/CopyOnWriteArrayList.png)

The name is self-explanatory. All modification operations on the collection (add, set, remove) result in the creation of a new copy of the internal array. This ensures that when an iterator traverses the collection, a ConcurrentModificationException will not be thrown. It is important to note that only references to objects are copied during the array copy (shallow copy), meaning that access to the fields of elements is not thread-safe. CopyOnWrite collections are particularly useful when write operations are infrequent, such as when implementing a listener subscription mechanism and iterating through the listeners.

**CopyOnWriteArrayList<E>** — A thread-safe analogue of ArrayList, implemented with the CopyOnWrite algorithm.

**CopyOnWriteArraySet<E>** — Implementation of the Set interface, using CopyOnWriteArrayList as a basis. Unlike CopyOnWriteArrayList, there are no additional methods.

### Examples:

[ConcurrentSkipListSet](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/collections/ConcurrentSkipListSetExample.java)\
[CopyOnWriteArrayList](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/collections/CopyOnWriteArrayListExample.java)

# Scalable Maps

![image](images/ConcurrentMap.png)

Improved implementations of HashMap, TreeMap with better support for multithreading and scalability.

**ConcurrentMap<K, V>** — An interface that extends Map with several additional atomic operations.

**ConcurrentHashMap<K, V>** — Unlike Hashtable and synchronized blocks on HashMap, data is represented as segments, broken down by key hashes. As a result, access to data is locked by segments, not by a single object. In addition, iterators represent data for a specific time slice and do not throw ConcurrentModificationException. More details ConcurrentHashMap

### Additional constructor

**ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel)** - The third parameter in the constructor represents the anticipated number of concurrent writing threads, with a default value of 16. This parameter significantly impacts the collection's memory footprint and performance.

**ConcurrentNavigableMap<K,V>** - This interface extends the NavigableMap interface and mandates that objects implementing ConcurrentNavigableMap are used as return values. All iterators provided by this interface are designated as safe for use and are programmed not to throw ConcurrentModificationException.

**ConcurrentSkipListMap<K, V>** - This class serves as a thread-safe equivalent of TreeMap. It organizes data based on keys and ensures an average performance of log(N) for operations like containsKey, get, put, remove, and similar operations.

**ConcurrentSkipListSet<E>** - This class implements the Set interface and is built upon ConcurrentSkipListMap for thread-safe set operations.

### Examples:

[ConcurrentHashMap](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/collections/ConcurrentHashMapExample.java)\
[ConcurrentSkipListMap](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/collections/ConcurrentSkipListMapExample.java)

# Queues

### Non-Blocking Queues

![image](images/AbstractQueue.png)

Thread-safe and non-blocking queue implementations based on linked nodes.

**ConcurrentLinkedQueue<E>** — This implementation utilizes the wait-free algorithm devised by Michael & Scott, optimized to work efficiently with the garbage collector. Built on CAS, this algorithm ensures high-speed operations. However, it's worth noting that the size() method may incur significant overhead if called frequently, so it's advisable to minimize its usage.

**ConcurrentLinkedDeque<E>** — Deque, pronounced as “Deck”, stands for Double-ended queue, indicating that data can be added to and removed from both ends. Consequently, this class supports both FIFO (First In First Out) and LIFO (Last In First Out) modes of operation. In practical scenarios, ConcurrentLinkedDeque should be employed only if LIFO functionality is indispensable, as its bidirectional nature causes a 40% performance loss compared to ConcurrentLinkedQueue.

### Blocking Queues
![image](images/BlockingQueue.png)

**BlockingQueue<E>** — When managing large data streams with queues, ConcurrentLinkedQueue alone may not suffice. If threads clearing the queue fail to keep up with the data influx, it could lead to memory exhaustion or significant IO/Net overload, causing a performance drop until system failure due to timeouts or lack of free descriptors. To address such scenarios, a queue with customizable size or conditional locking is necessary. This is where the BlockingQueue interface comes in, providing access to a range of useful classes. Besides setting the queue size, new methods have been introduced to handle underfilling or overflowing queues differently. For instance, when adding an element to a full queue, one method throws an IllegalStateException, another returns false, another blocks the thread until space is available, and yet another blocks the thread with a timeout, returning false if space is still unavailable. It's important to note that blocking queues don't support null values since null is used in the poll method as a timeout indicator.

**ArrayBlockingQueue<E>** — A blocking queue implemented using a traditional ring buffer. In addition to the queue size, it allows control over lock fairness. If fair=false (default), thread order is not guaranteed. Refer to the description of ReentrantLock for more on "fairness."

**DelayQueue<E extends Delayed>** — A specialized class that retrieves elements from the queue only after a delay specified in each element via the getDelay method of the Delayed interface.

**LinkedBlockingQueue<E>** — A blocking queue implemented with linked nodes, using the "two lock queue" algorithm: one lock for adding, another for removing elements. Compared to ArrayBlockingQueue, this class offers higher performance due to two locks, but consumes more memory. The queue size is set via the constructor and defaults to Integer.MAX_VALUE.

**PriorityBlockingQueue<E>** — A thread-safe wrapper over PriorityQueue. When inserting an element, its position in the queue is determined by the Comparator logic or the Comparable interface implemented in the elements. The smallest element is dequeued first.

**SynchronousQueue<E>** — Operates on a "one in, one out" principle. Each insert operation blocks the producer thread until the consumer thread retrieves an element, and vice versa; the consumer waits until the producer inserts an element.

**BlockingDeque<E>** — An interface providing additional methods for a bidirectional blocking queue, allowing data insertion and retrieval from both ends of the queue.

**LinkedBlockingDeque<E>** — A bidirectional blocking queue implemented with linked nodes, essentially a doubly linked list with a single lock. The queue size is specified via the constructor and defaults to Integer.MAX_VALUE.

**TransferQueue<E>** — This interface is interesting because it allows blocking the producer thread when adding an element until a consumer thread retrieves an element from the queue. The blocking can include a timeout or a check for waiting consumers, enabling synchronous and asynchronous message transfer mechanisms.

**LinkedTransferQueue<E>** — An implementation of TransferQueue based on the Dual Queues with Slack algorithm, utilizing CAS and thread parking extensively when idle.

### Examples:

[ArrayBlockingQueue example](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/queue/ArrayBlockingQueueExample.java)\
[ConcurrentLinkedDeque example](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/queue/ConcurrentLinkedDequeExample.java)\
[ConcurrentLinkedQueue example](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/queue/ConcurrentLinkedQueueExample.java)\
[BlockingQueue Producer-Consumer example](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/collections/BlockingQueueSimpleExample.java)\
[CustomBlockingQueue example](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/customblockingqueue/CustomBlockingQueue.java)

# Synchronizers
![image](images/Synchronizers.png)

This section introduces classes for active thread management:

**Semaphore** - Typically used to limit the number of threads accessing hardware resources or a file system. A counter controls access to a shared resource. If the counter is greater than zero, access is granted, and the counter is decremented. If the counter is zero, the current thread is blocked until another thread releases the resource. The number of permits and the "fairness" of thread release are specified via the constructor. The challenge with semaphores is setting the number of permits, often depending on hardware capabilities.

**CountDownLatch** - Allows one or more threads to wait until a specific number of operations in other threads are completed. For example, threads calling the latch's await method (with or without a timeout) will block until another thread completes initialization and calls the countDown method. This method decrements the count. When the counter reaches zero, all waiting threads proceed, and subsequent await calls pass without waiting. The count is one-time and cannot be reset.

**CyclicBarrier** - Used to synchronize a set number of threads at a common point. The barrier is reached when N threads call the await method and block. The counter then resets, and waiting threads are released. Optionally, a Runnable task can be executed before threads are unblocked and the counter is reset.

**Exchanger<V>** - Facilitates the exchange of objects between two threads, supporting null values for single object transfers or as a simple synchronizer. The first thread calling the exchange method blocks until the second thread calls the same method. The threads then exchange values and proceed.

**Phaser** - An advanced barrier for thread synchronization, combining features of CyclicBarrier and CountDownLatch. The number of threads is dynamic and can change. The class can be reused and allows threads to report readiness without blocking.

### Examples:

[Barrier wrapper](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/synchronizers/Barrier.java)\
[Barrier](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/synchronizers/BarrierExample.java)\
[CountDownLatch](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/synchronizers/CountDownLatchExample.java)\
[Exchanger](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/synchronizers/ExchangerExample.java)\
[Phaser](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/synchronizers/PhaserExample.java)\
[SemaphorePrintQueue](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/synchronizers/SemaphorePrintQueueExample.java)

# Executors

Here, we reach the most extensive section of the package. This part covers interfaces for executing asynchronous tasks with the capability of receiving results via the Future and Callable interfaces. Additionally, it includes services and factories for creating thread pools such as ThreadPoolExecutor, ScheduledThreadPoolExecutor, and ForkJoinPool. To enhance comprehension, we will break down the interfaces and classes into smaller, more manageable parts.
### Future and Callable

![image](images/Future.png)

**Future<V>** — This is a useful interface for obtaining the results of an asynchronous operation. The key method is get, which blocks the current thread (with or without a timeout) until the asynchronous operation completes in another thread. Additional methods are available for canceling the operation and checking its current status. The FutureTask class often implements this interface.

**RunnableFuture<V>** — While Future serves as a Client API interface, the RunnableFuture interface is used to start the asynchronous operation. The successful completion of the run() method marks the asynchronous operation as complete, allowing the results to be retrieved via the get method.

**Callable<V>** — This is an extended version of the Runnable interface for asynchronous operations. It allows returning a typed value and throwing a checked exception. Although it lacks a run() method, many java.util.concurrent classes support it along with Runnable.

**FutureTask<V>** — This class implements the Future and RunnableFuture interfaces. It accepts an asynchronous operation as input in the form of Runnable or Callable objects. The FutureTask class is designed to be launched in a worker thread, for example, via new Thread(task).start(), or through a ThreadPoolExecutor. The results of the asynchronous operation are retrieved using the get(...) method.

**Delayed** — This interface is used for asynchronous tasks that should start in the future, as well as in DelayQueue. It allows setting the time before the start of an asynchronous operation.

**ScheduledFuture<V>** — A marker interface that combines the functionalities of Future and Delayed.

**RunnableScheduledFuture<V>** — An interface that combines RunnableFuture and ScheduledFuture. It also allows specifying whether the task is one-time or should be launched at a specified frequency.

### Executor Services

![image](images/Executor.png)

**Executor** — This is the fundamental interface for classes that execute Runnable tasks. It decouples the task submission process from the execution mechanism.

**ExecutorService** — An interface that defines a service for executing Runnable or Callable tasks. The submit methods take a task as a Callable or Runnable and return a Future through which the result can be obtained. The invokeAll methods handle lists of tasks, blocking the thread until all tasks in the provided list are completed or the specified timeout expires. The invokeAny methods block the calling thread until any one of the passed tasks completes. The interface also includes methods for graceful shutdown. Once the shutdown method is called, the service will no longer accept new tasks and will throw a RejectedExecutionException if an attempt is made to submit a task.

**ScheduledExecutorService** — This interface extends ExecutorService by adding capabilities for scheduling tasks to be executed after a delay or periodically.

**AbstractExecutorService** — An abstract class that serves as a base for building an ExecutorService. It provides the basic implementation of the submit, invokeAll, and invokeAny methods. Classes such as ThreadPoolExecutor, ScheduledThreadPoolExecutor, and ForkJoinPool inherit from this class.

### Examples:

[Custom ExecutorService implementation](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/executors/AbstractExecutorServiceExample.java)\
[ExecutorCompletionService](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/executors/CompletionServiceExample.java)\
[Executors example](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/executors/ExecutorsExample.java)\
[ExecutorService](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/executors/ExecutorService.java)\
[ScheduledThreadPoolExecutors](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/executors/ScheduledThreadPoolExecutorExample.java)\
[ThreadPoolExecutors](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/executors/ThreadPoolExecutorExample.java)

# ThreadPoolExecutor & Factory

![image](images/AbstractExecutorService.png)

**ThreadPoolExecutor** — A highly versatile and essential class used to execute asynchronous tasks within a thread pool. This approach minimizes the overhead associated with creating and terminating threads. By maintaining a fixed maximum number of threads in the pool, it ensures predictable application performance. It is generally recommended to create this pool using one of the factory methods provided by the Executors class. However, if the standard configurations are insufficient, all key parameters of the pool can be set via constructors or setters. For more details, refer to the relevant documentation.

**ScheduledThreadPoolExecutor** — In addition to the methods of ThreadPoolExecutor, this class allows tasks to be scheduled for execution after a specific delay or at a fixed rate, enabling the implementation of a timer service based on this class.

**ThreadFactory** — By default, ThreadPoolExecutor uses the standard thread factory obtained through Executors.defaultThreadFactory(). If additional customization is needed, such as setting thread priority or naming threads, you can implement this interface and pass it to ThreadPoolExecutor.

**RejectedExecutionHandler** — Defines a handler for tasks that cannot be executed by ThreadPoolExecutor for various reasons, such as a lack of available threads or the service being shut down. The ThreadPoolExecutor class includes several standard implementations: CallerRunsPolicy — runs the task in the calling thread; AbortPolicy — throws an exception; DiscardPolicy — silently discards the task; DiscardOldestPolicy — removes the oldest unexecuted task from the queue and retries adding the new task.

# Fork Join

![image](images/AbstractExecutorService1.png)

Java 1.7 introduces a new Fork Join framework for solving recursive problems using divide and conquer or Map Reduce algorithms.

Thus, by dividing into parts, it is possible to achieve their parallel processing in different threads. To solve this problem, you can use the usual ThreadPoolExecutor, but due to frequent context switching and tracking of execution control, all this does not work very effectively. Here, the Fork Join framework comes to our aid, which is based on the work-stealing algorithm. It reveals itself best in systems with a large number of processors. You can read more in the blog here or in Doug Lea's publication. You can read about performance and scalability here.

**ForkJoinPool** — The main entry point for initiating root (main) ForkJoinTask tasks. Subtasks are started using methods of the task being forked. By default, the thread pool is created with a number of threads equal to the number of processors (cores) available to the JVM.

**ForkJoinTask** — The base class for all Fork/Join tasks. Key methods include: fork() — adds a task to the queue of the current ForkJoinWorkerThread for asynchronous execution; invoke() — executes a task in the current thread; join() — waits for the subtask to complete and returns the result; invokeAll(…) — combines the previous three operations, executing two or more tasks at once; adapt(…) — creates a new ForkJoinTask from Runnable or Callable objects.

**RecursiveTask** — An abstract class derived from ForkJoinTask, requiring the implementation of the compute method, which performs the asynchronous operation.

**RecursiveAction** — Similar to RecursiveTask but does not return a result.

**ForkJoinWorkerThread** — Used as the default implementation in ForkJoinPool. Optionally, it can be extended to override worker thread initialization and completion methods.

# Completion Service

![image](images/CompletionService.png)

**CompletionService** — An interface that separates the submission of asynchronous tasks from the retrieval of their results. The submit methods are used to add tasks, while the take method (blocking) and poll method (non-blocking) are used to obtain the results of completed tasks.

**ExecutorCompletionService** — A wrapper around any class that implements the Executor interface, such as ThreadPoolExecutor or ForkJoinPool. It is primarily used to abstract the task submission and execution monitoring process. If tasks are completed, their results can be retrieved; otherwise, the take method will wait for completion. The default service uses LinkedBlockingQueue, but any BlockingQueue implementation can be used.

# Locks

![image](images/Locks.png)

**Condition** — An interface that provides alternative methods to the traditional wait/notify/notifyAll methods. A condition object is typically obtained from a lock using the lock.newCondition() method, allowing multiple wait/notify sets for a single object.

**Lock** — A fundamental interface in the lock framework that offers a more flexible approach to controlling access to resources or blocks compared to using synchronized. When using multiple locks, the release order can be arbitrary, and it provides an option to follow an alternative scenario if the lock is already held by another thread.

**ReentrantLock** — A reentrant lock that allows only one thread to enter a protected block at a time. This class supports both "fair" and "non-fair" thread locking. With "fair" locking, threads are released in the order they called lock(). With "unfair" locking, the release order is not guaranteed, but it operates faster. By default, "unfair" locking is used.

**ReadWriteLock** — An interface for creating read/write locks. These locks are particularly useful when the system has many read operations and few write operations.

**ReentrantReadWriteLock** — Commonly used in multithreaded services and caches, providing a significant performance improvement over synchronized blocks. This class operates in two mutually exclusive modes: multiple readers can read data simultaneously, while only one writer can write data at a time.

**ReentrantReadWriteLock.ReadLock** — A read lock for readers, obtained via readWriteLock.readLock().

**ReentrantReadWriteLock.WriteLock** — A write lock for writers, obtained via readWriteLock.writeLock().

**LockSupport** — Designed for creating classes with locks. It includes methods for parking threads, serving as replacements for the deprecated Thread.suspend() and Thread.resume() methods.

### Examples:

[ReadWriteLock](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/locks/ReadWriteLockExample.java)\
[ReentrantReadWriteLockCounter](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/locks/ReentrantReadWriteLockCounter.java)\
[ReentrantReadWriteLockCounter](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/locks/ReentrantReadWriteLockCounterExample.java)\
[AbstractOwnableSynchronizer](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/locks/AbstractOwnableSynchronizerExample.java)\
[AbstractQueuedLongSynchronizer](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/locks/AbstractQueuedLongSynchronizerExample.java)\
[AbstractQueuedSynchronizer](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/locks/AbstractQueuedSynchronizerExample.java)\
[LockSupport](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/locks/LockSupportExample.java)

![image](images/AbstractOwnableSynchronizer.png)

**AbstractOwnableSynchronizer** — A base class designed for creating synchronization mechanisms. It includes a simple getter/setter pair for storing and accessing an exclusive thread that can interact with the data.

**AbstractQueuedSynchronizer** — This class serves as the foundation for synchronization mechanisms in FutureTask, CountDownLatch, Semaphore, ReentrantLock, and ReentrantReadWriteLock. It can also be used to develop new synchronization mechanisms that rely on a single atomic integer value.

**AbstractQueuedLongSynchronizer** — A variant of AbstractQueuedSynchronizer that supports operations on an atomic long value.

# Atomics

![image](images/Atomics.png)


**AtomicBoolean, AtomicInteger, AtomicLong, AtomicIntegerArray, AtomicLongArray** — When you need to synchronize access to a simple int variable in a class, you can use synchronized constructs, or volatile with atomic set/get operations. However, the new Atomic* classes offer an even better solution. These classes use CAS (Compare-And-Swap) operations, which are faster than synchronization with synchronized or volatile. Additionally, they provide methods for atomic addition, increment, and decrement.

**AtomicReference** — This class allows for atomic operations on an object reference.

**AtomicMarkableReference** — This class supports atomic operations on a pair of fields: an object reference and a boolean flag (true/false).

**AtomicStampedReference** — This class supports atomic operations on a pair of fields: an object reference and an integer value.

**AtomicReferenceArray** — An array of object references that can be updated atomically.

**AtomicIntegerFieldUpdater, AtomicLongFieldUpdater, AtomicReferenceFieldUpdater** — These classes allow for atomic updates of fields by their names using reflection. The field offsets for CAS are determined in the constructor and cached, so the performance impact of reflection is minimal.

### Examples:

[Atomics](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicExample.java)\
[AtomicIntegerFieldUpdater](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicIntegerFieldUpdaterExample.java)\
[AtomicLongFieldUpdater](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicLongFieldUpdaterExample.java)\
[AtomicMarkableReference](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicMarkableReferenceExample.java)\
[AtomicReferenceArray](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicReferenceArrayExample.java)\
[AtomicReference](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicReferenceExample.java)\
[AtomicReferenceFieldUpdater](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicReferenceFieldUpdaterExample.java)\
[AtomicStampedReference](https://github.com/alxkm/java-concurrency-patterns/blob/main/src/main/java/org/alxkm/patterns/atomics/AtomicStampedReferenceExample.java)

Some images [source](http://habrahabr.ru/company/luxoft/blog/157273/)

### Installation
Include this library in your project by adding the respective files to your classpath.

### Requirements 
Java 8 or higher is required to use this library.
 
### Testing

The repository includes JUnit tests that validate the functionality of each cache implementation. These tests cover repository described functionality.

### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.

Feel free to fork and modify these implementations for your own use cases or contribute to enhance them further. If you have any questions or suggestions, please feel free to reach out or open an issue!

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.

## Acknowledgments

This repository was inspired by multithreading technics and adapted for educational purposes.

## Contact

For any questions or suggestions, please feel free to reach out or open an issue!
