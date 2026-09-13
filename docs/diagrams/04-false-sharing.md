# False sharing: two fields, no contention, 2.8x slower

Caches move data in **lines**, typically 64 bytes, not in fields. Two fields on the same line are
one unit as far as the coherence protocol is concerned.

## The layout

```mermaid
flowchart TD
    subgraph L1["one 64-byte cache line"]
        F1["first"]
        F2["second"]
    end
    C0["core 0<br/>writes first"] --> F1
    C1["core 1<br/>writes second"] --> F2
    L1 --> X["Neither thread touches<br/>the other's field.<br/>Both are invalidated<br/>on every write anyway."]
```

## The ping-pong

```mermaid
sequenceDiagram
    participant C0 as core 0
    participant LINE as the cache line
    participant C1 as core 1

    C0->>LINE: first++, take exclusive
    C1->>LINE: second++, invalidate core 0
    C0->>LINE: first++, refetch and take again
    C1->>LINE: second++, invalidate again
    Note over C0,C1: every increment pays a cache miss it did not need
```

## Padded apart

```mermaid
flowchart TD
    subgraph LA["cache line 1"]
        A1["first"]
        A2["7 longs of padding"]
    end
    subgraph LB["cache line 2"]
        B1["second"]
    end
    D0["core 0"] --> A1
    D1["core 1"] --> B1
    LB --> Y["independent lines,<br/>no coherence traffic"]
```

7 longs at 8 bytes each, plus the field itself, fills a 64-byte line, so `second` cannot land on
line 1.

## Measured here

50,000,000 increments per thread, 12 cores, both layouts compiled before anything is timed:

| Layout | Elapsed |
|---|---|
| adjacent | ~730 ms |
| padded | ~265 ms |

About **2.8x**.

> **Warm the JIT up first.** The earlier version of this demo timed the very first run of each
> layout and reported 3x. That was the adjacent loop being compiled, not a cache effect: the second
> round showed no difference at all. A real ratio and a warm-up artefact look identical from the
> outside.

Correctness is identical either way. Each counter is written by exactly one thread, so no increment
is ever lost. False sharing is purely a performance effect, which is what makes it easy to miss:
nothing is wrong, everything is just slow.

## Where you already rely on this

```mermaid
flowchart LR
    A["AtomicLong<br/>one counter"] --> B["every thread CASes<br/>the same cache line"]
    C["LongAdder<br/>cell 0, cell 1, cell 2, cell 3"] --> D["each padded onto its own line,<br/>summed on read"]
    B --> E["115 ops/us at 8 threads"]
    D --> F["1256 ops/us at 8 threads"]
```

The catch: `sum()` walks every cell. A counter read as often as written is a different question.

Padding is not a general optimisation. It costs memory and only pays where different threads write
neighbouring fields hard. Measure first.

> Source: `src/main/java/org/alxkm/memorymodel/FalseSharingExample.java`
