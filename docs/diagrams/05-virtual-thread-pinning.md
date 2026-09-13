# Virtual thread pinning: where synchronized stops being free

A virtual thread that blocks normally **unmounts** from its carrier. That is the whole trick: a
handful of carriers serve thousands of virtual threads.

## Normal blocking

```mermaid
sequenceDiagram
    participant C as carrier 1
    participant V1 as virtual thread 1
    participant V9 as virtual thread 9

    V1->>C: mounted, running
    V1->>V1: lock.lock(), then blocks
    V1-->>C: UNMOUNTS, parked off-heap
    Note over C: carrier is free
    V9->>C: mounts, runs
    Note over C,V9: 12 carriers serve all 64 tasks
```

## Pinned, on Java 21

```mermaid
sequenceDiagram
    participant C as carrier 1
    participant V1 as virtual thread 1
    participant V9 as virtual thread 9

    V1->>C: mounted, running
    V1->>V1: enters synchronized, then blocks
    Note over V1,C: monitor is tied to the CARRIER
    V1--xC: cannot unmount
    Note over C: carrier held while doing nothing
    V9--xC: waits for a free carrier
    Note over C,V9: only 12 of 64 tasks in flight
```

## Measured here

64 tasks, each blocking 500 ms, on 12 cores. Every task locks a monitor **of its own**, so there is
no lock contention at all.

| | Elapsed | Why |
|---|---|---|
| `synchronized` | **3060 ms** | 12 at a time, so six rounds |
| `ReentrantLock` | **503 ms** | all 64 at once |

About **6x**. What runs out is carriers, not locks. That is the part people miss when they look for
contention and find none.

## How to see it in your own code

Pinning is invisible in a thread dump. The symptom is throughput that refuses to scale.

```mermaid
flowchart TD
    A["Throughput will not scale,<br/>and there is no lock contention"] --> B["Run with<br/>-Djdk.tracePinnedThreads=short"]
    B --> C["Prints the frame<br/>holding the monitor"]
    A --> D["In production, use the<br/>jdk.VirtualThreadPinned JFR event"]
    D --> E["Same information,<br/>far less overhead"]
    C --> F{"Is the blocking call<br/>inside synchronized?"}
    F -->|yes| G["Swap the monitor<br/>for a ReentrantLock"]
    F -->|no| H["Look elsewhere:<br/>native frames also pin"]
```

```
Thread[#97,ForkJoinPool-1-worker-12,5,CarrierThreads]
    VirtualThreadPinningExample.lambda$runPinned$0(...) <== monitors:1
```

## The fix, and its expiry date

```java
synchronized (lock) {          ->      lock.lock();
    blockingCall();                    try { blockingCall(); }
}                                      finally { lock.unlock(); }
```

A virtual thread can hold a `ReentrantLock` across an unmount. Everywhere else `synchronized` is
fine, including when nothing blocks inside it.

| Version | Behaviour |
|---|---|
| Java 21, current LTS, what this repo builds against | pins, this matters |
| Java 24 and later, JEP 491 | no longer pins |

Check your target before rewriting anything.

> Source: `src/main/java/org/alxkm/diagnostics/VirtualThreadPinningExample.java`
