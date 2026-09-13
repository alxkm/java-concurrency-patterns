# Instruction reordering: the outcome the source forbids

Two threads, two plain fields, both starting at 0.

```
actor 1                      actor 2
-------                      -------
x = 1;                       y = 1;
r1 = y;                      r2 = x;
```

Read that literally and `(0, 0)` looks impossible:

```mermaid
flowchart LR
    A["actor1 runs first"] --> B["0, 1"]
    C["actor2 runs first"] --> D["1, 0"]
    E["they interleave"] --> F["1, 1"]
    G["0, 0"] --> H["would need both loads<br/>before both stores.<br/>No interleaving allows it."]
```

## It happens anyway

```mermaid
sequenceDiagram
    participant C0 as core 0
    participant SB0 as store buffer 0
    participant MEM as memory
    participant SB1 as store buffer 1
    participant C1 as core 1

    C0->>SB0: x = 1 (buffered, not visible yet)
    C1->>SB1: y = 1 (buffered, not visible yet)
    C0->>MEM: r1 = y
    MEM-->>C0: 0
    C1->>MEM: r2 = x
    MEM-->>C1: 0
    SB0->>MEM: x drains, too late
    SB1->>MEM: y drains, too late
    Note over C0,C1: observed 0, 0
```

The store sits in the writing core's buffer while that core proceeds to its load. Both loads can
execute before either store becomes visible. This is StoreLoad reordering, x86 does it, and the JMM
permits it because there is no happens-before edge between the two threads.

This is the concrete answer to "what does `volatile` actually prevent".

## Why this is not a unit test

```mermaid
flowchart TD
    A["Want to catch reordering<br/>in a unit test"] --> B["Two threads must line up"]
    B --> C["Lining them up needs<br/>a barrier or a latch"]
    C --> D["That IS a memory barrier"]
    D --> E["It drains the store buffer<br/>that produces the effect"]
    E --> F["The test destroys<br/>what it is measuring"]
    F --> G["0 reorderings in 20,000 runs<br/>0 in 500,000 barrier iterations"]
    G --> H["Use jcstress instead"]
```

jcstress spins the actors without synchronisation and samples in bulk:

| Result | Samples | Frequency | Expect |
|---|---|---|---|
| `0, 0` | **9,914,377** | **3.74%** | Interesting, the reordering |
| `0, 1` | 126,435,416 | 47.65% | Acceptable |
| `1, 0` | 128,998,594 | 48.61% | Acceptable |

Marking the fields `volatile` makes `0, 0` **FORBIDDEN**, and the run fails if it is ever seen. The
guarantee becomes something checked rather than asserted in prose.

Zero by hand, millions under jcstress. These bugs do not fail loudly in testing. They fail in
production, rarely, on someone else's hardware.

> Source: `src/jcstress/java/org/alxkm/memorymodel/ReorderingStressTest.java`, run with `./gradlew jcstress`
