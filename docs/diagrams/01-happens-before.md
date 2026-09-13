# Happens-before: the four edges you get for free

If a write happens-before a read, the read **must** see that write. If no edge exists, the read may
see it, may see a stale value, or may see actions in a different order than the source lists them.

Four edges arrive without any special effort.

## 1. Thread.start()

Everything the starting thread did before `start()` is visible inside the new thread.

```mermaid
sequenceDiagram
    participant M as main
    participant W as worker
    M->>M: data = 42
    M->>W: start()
    Note over M,W: happens-before edge
    W->>W: reads data
    W-->>M: sees 42, guaranteed
```

## 2. Thread.join()

Everything the joined thread did is visible after `join()` returns.

```mermaid
sequenceDiagram
    participant M as main
    participant W as worker
    M->>W: start()
    W->>W: data = 42
    W-->>M: join() returns
    Note over M,W: happens-before edge
    M->>M: reads data, sees 42
```

This is the edge that lets the tests here read a counter after joining its writers without
synchronising the read. Worth recognising, so you stop adding `volatile` to fields that already
have an edge.

## 3. volatile write to volatile read

The edge carries **everything written before it**, not just the volatile field itself.

```mermaid
sequenceDiagram
    participant W as writer
    participant R as reader
    W->>W: payload = 42 (plain field!)
    W->>W: ready = true (volatile)
    Note over W,R: the volatile write publishes the plain one too
    R->>R: while (!ready) spin
    R->>R: reads payload
    R-->>W: sees 42, guaranteed
```

This is the mechanism behind double-checked locking, behind every "flag plus payload" handover, and
behind every lazy singleton. Take `volatile` off the flag and a reader can see the flag set while
the payload is still zero.

## 4. unlock to lock, on the SAME lock

```mermaid
sequenceDiagram
    participant W as writer
    participant R as reader
    W->>W: lock()
    W->>W: data = 42
    W->>W: unlock()
    Note over W,R: happens-before edge, same monitor only
    R->>R: lock()
    R->>R: reads data, sees 42
```

"The same lock" is the load-bearing part.

```mermaid
flowchart LR
    A["field guarded by<br/>synchronized in one method"] -->|and| B["a ReentrantLock<br/>in another"]
    B --> C["guarded by NEITHER"]
    C --> D["The two mechanisms do not exclude<br/>each other and publish nothing<br/>to one another"]
```

That bug was in this repository's own `AccountAmount` until it was fixed.

> Source: `src/main/java/org/alxkm/memorymodel/HappensBeforeExample.java`
