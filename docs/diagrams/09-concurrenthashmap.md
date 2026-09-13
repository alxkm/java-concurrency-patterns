# ConcurrentHashMap: what changed in Java 8

The most repeated out-of-date fact in Java concurrency. This repository repeated it too, until the
text was corrected. `images/ConcurrentMap.png` still shows the Java 7 design.

## Java 7 and earlier: segments

```mermaid
flowchart TD
    CHM["ConcurrentHashMap"] --> S0["Segment 0<br/>own ReentrantLock<br/>own table"]
    CHM --> S1["Segment 1<br/>own ReentrantLock<br/>own table"]
    CHM --> S2["Segment 2<br/>own ReentrantLock<br/>own table"]
    CHM --> S15["Segment 15<br/>own ReentrantLock<br/>own table"]

    WA["writer A"] --> S0
    WB["writer B"] --> S0
    WC["writer C"] --> S2

    S0 --> X["A holds the lock,<br/>B waits, even though<br/>their buckets differ"]
```

A fixed array of mini hashtables, each with its own lock. `concurrencyLevel`, default 16, **set the
number of segments**.

Consequence: at most 16 writers could proceed at once, however large the map. Two keys landing in
the same segment blocked each other even with thousands of free buckets.

## Java 8 and later: no segments at all

```mermaid
flowchart TD
    CHM["ConcurrentHashMap"] --> T["one table of bins"]
    T --> B0["bin 0: empty"]
    T --> B1["bin 1: Node -> Node"]
    T --> B2["bin 2: empty"]
    T --> BN["bin n: TreeBin"]

    B0 --> C0["CAS the head in.<br/>No lock taken at all."]
    B1 --> C1["synchronized on<br/>THAT node only"]
    BN --> CN["bin longer than 8 becomes<br/>a red-black tree.<br/>Bad hashes degrade to<br/>O(log n), not O(n)."]
```

The lock granularity is the **bin**, not a segment. Two writers collide only when their keys hash to
the same bucket, so concurrency scales with table size instead of being capped at 16.

`concurrencyLevel` still exists in the constructor, but it is now only a **sizing hint** for the
initial table. It no longer controls how many writers can proceed.

## Three details worth carrying

| | |
|---|---|
| empty bin | CAS, lock-free on the common path |
| occupied bin | `synchronized` on the first node |
| bin over 8 long | becomes a red-black tree, which is what makes hash collisions survivable |

`size()` is not a field. It is a striped counter summed on read, the same idea as `LongAdder`, which
is why it is an estimate under concurrent modification and `mappingCount()` is the long-returning
version to prefer.

## Iterators: unchanged, and still misunderstood

Both versions give **weakly consistent** iterators.

```mermaid
flowchart LR
    A["never throws<br/>ConcurrentModificationException"] --> B["often read as<br/>'gives a consistent view'"]
    B --> C["It does not."]
    C --> D["Reflects the map at some point<br/>during traversal, not a snapshot.<br/>An entry added mid-iteration<br/>may or may not appear."]
```

If you need a stable view, you have to build one.

## Choosing

```mermaid
flowchart TD
    A["keyed access, concurrent"] --> B{"Need sorted keys?"}
    B -->|yes| C["ConcurrentSkipListMap<br/>O(log n), no locks"]
    B -->|no| D{"Need a consistent<br/>snapshot?"}
    D -->|yes| E["Neither. Copy under<br/>your own lock."]
    D -->|no| F["ConcurrentHashMap"]
```

Do not call `compute` or `merge` and then touch another map inside the lambda. The bin is locked
while your function runs; re-entering the same map can deadlock, and a slow lambda holds up every
writer hashing to that bin.

> README section "Scalable Maps". Replaces `images/ConcurrentMap.png`.
