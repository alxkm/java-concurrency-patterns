# Choosing: what to reach for, and what it costs

Every number here was measured in this repository. Reproduce with `./gradlew jmh`.

## A shared counter

```mermaid
flowchart TD
    A["I need a shared counter"] --> B{"Read as often<br/>as written?"}
    B -->|yes| C["AtomicLong<br/>115 ops/us at 8 threads"]
    B -->|no, write heavy| D{"Contended?"}
    D -->|yes| E["LongAdder<br/>1256 ops/us at 8 threads"]
    D -->|no| F["AtomicLong<br/>204 ops/us single threaded"]
    A --> G{"Counter plus other<br/>state, atomically<br/>together?"}
    G -->|yes| H["A lock.<br/>Atomics guard one field only"]
    G --> I{"Need tryLock,<br/>fairness or a Condition?"}
    I -->|yes| J["ReentrantLock<br/>67 ops/us at 8 threads"]
    I -->|no| K["synchronized is fine<br/>18 ops/us at 8 threads"]
```

| | 1 thread | 8 threads |
|---|---|---|
| `synchronized` | 92.9 | 18.2 |
| `ReentrantLock` | 96.1 | 66.6 |
| `AtomicLong` | 204.2 | 115.5 |
| `LongAdder` | 210.3 | **1256.5** |

Uncontended, the atomics are already about 2x ahead. The old advice that an uncontended monitor is
nearly free assumed biased locking, disabled in JDK 15 and removed in 18.

`LongAdder` wins by spreading its state across padded cells so threads stop fighting over one cache
line. The catch: `sum()` walks every cell, so a counter read as often as written is a different
question.

## A queue between threads

```mermaid
flowchart TD
    A["I need a queue<br/>between threads"] --> B{"Need blocking,<br/>i.e. backpressure?"}

    B -->|no| C{"Need LIFO too?"}
    C -->|no| D["ConcurrentLinkedQueue<br/>4.4 ops/us"]
    C -->|yes| E["ConcurrentLinkedDeque<br/>3.2 ops/us, about 27% slower"]

    B -->|yes| F{"Special delivery<br/>order?"}
    F -->|by priority| G["PriorityBlockingQueue"]
    F -->|after a delay| H["DelayQueue"]
    F -->|producer must know<br/>it was picked up| I["LinkedTransferQueue"]
    F -->|plain FIFO| J["ArrayBlockingQueue<br/>23.6 ops/us"]
```

Note the last one. This README used to claim `LinkedBlockingQueue` is faster thanks to its two-lock
design. Measured, `ArrayBlockingQueue` was ahead both single-threaded (23.6 against 11.1) and with
four producers against four consumers (49.5 against 40.9 total, and the linked queue's error bar was
18.2 against 1.4). It allocates a node per element; the array queue reuses a ring buffer.

Bounded is a feature. An unbounded queue turns a slow consumer into an `OutOfMemoryError` instead of
backpressure.

## A shared list

```mermaid
flowchart TD
    A["I need a shared list"] --> B{"Reads dominate?"}
    B -->|yes| C["CopyOnWriteArrayList<br/>reads take no lock at all"]
    B -->|writes are frequent| D{"How large?"}
    D -->|small| C
    D -->|large| E["synchronizedList,<br/>or rethink the structure"]
    A --> F{"Keyed access<br/>rather than indexed?"}
    F -->|yes| G["ConcurrentHashMap,<br/>not a list"]
```

1000 elements, group throughput in ops/us:

| | 7 readers, 1 writer | 4 readers, 4 writers |
|---|---|---|
| `CopyOnWriteArrayList` | 314.7 | 300.3 |
| `Collections.synchronizedList` | 19.3 | 12.4 |

"Useful when writes are infrequent" is right but understates it: even at an even split CopyOnWrite
was 24x ahead, because its reads take no lock and reads dominate the total. Read the write column
separately though: 4.1 against 5.8 at 1000 elements, and every write copies the whole array, so it
degrades with size.

## Virtual or platform threads

```mermaid
flowchart TD
    A["Which kind of thread?"] --> B{"Does the task<br/>block on I/O?"}
    B -->|no, CPU bound| C["Platform threads,<br/>pool sized to cores"]
    B -->|yes| D{"Does it block inside<br/>synchronized?"}
    D -->|no| E["Virtual threads"]
    D -->|yes| F{"Java 24 or later?"}
    F -->|yes, JEP 491| E
    F -->|no, Java 21| G["It PINS a carrier.<br/>Swap for ReentrantLock<br/>measured 6x here"]
```

> Source: `src/jmh/java/org/alxkm/benchmark/`
