# How a concurrency test lies to you

Every failure below was found in **this** repository and fixed. That is the point of the list: these
are not hypotheticals, they are what a green suite looked like while the code was broken.

```mermaid
flowchart TD
    A["My concurrency test passes"] --> B{"Does it sleep<br/>instead of waiting?"}
    B -->|yes| B1["Passes fast, fails on CI.<br/>Wait for the condition."]
    B -->|no| C{"Does it assert inside<br/>a spawned thread?"}
    C -->|yes| C1["AssertionError kills that thread only.<br/>JUnit never sees it.<br/>Return values to the test thread."]
    C -->|no| D{"Does it call the class<br/>it is named after?"}
    D -->|no| D1["It tests the JDK.<br/>407 lines here never mentioned<br/>the class under test."]
    D -->|yes| E{"Does it assert the<br/>outcome of a race?"}
    E -->|yes| E1["Passes, then fails next run.<br/>Order with a latch,<br/>assert the ordering."]
    E -->|no| F{"Is there a hard<br/>threshold on a timing?"}
    F -->|yes| F1["Coin flip.<br/>Compare two strategies instead."]
    F -->|no| G["Probably a real test"]
```

## The nine

| | Failure | Fix |
|---|---|---|
| 1 | `Thread.sleep(1000)` then assert | wait for the condition: `Await.until(...)` |
| 2 | `assertEquals` inside a spawned thread | return values, assert on the test thread |
| 3 | `assertTrue(x \|\| !x)`, a tautology | assert the property the code provides |
| 4 | test builds its own `AtomicInteger` and never calls the example | call the example; if it returns `void` and only prints, that is the bug |
| 5 | asserting which side of a race won | order the two with a latch |
| 6 | `assertTrue(cpuMillis > 50)` after a 100 ms spin | compare spinning against sleeping |
| 7 | using a barrier to catch reordering | the barrier drains the store buffer; use jcstress |
| 8 | `findDeadlockedThreads()` in an assertion | scope it: `deadlockedAmong(myThreads)` |
| 9 | `//@Test` | fix it, or `@Disabled` with a reason |

## Number 7 deserves its own picture

```mermaid
flowchart LR
    A["Two threads<br/>must line up"] --> B["Needs a barrier"]
    B --> C["A barrier IS<br/>a memory barrier"]
    C --> D["It drains the store buffer"]
    D --> E["Which is what produces<br/>the effect you wanted"]
    E --> F["0 in 20,000 runs<br/>0 in 500,000 iterations"]
```

Against jcstress on the same idiom: 9,914,377 observations, 3.74% of samples. Know when your tool
cannot see the thing.

## Number 9 is the expensive one

```mermaid
flowchart LR
    A["Test fails"] --> B["//@Test"]
    B --> C["Suite green"]
    C --> D["Bug stays"]
    D --> E["Message lost"]
```

Fourteen disabled tests were found here. Twelve were hiding one real bug: the Leader-Follower
pattern processed exactly one event and then spun forever. The tests had been right all along.

A commented-out `@Test` is invisible to every tool that could remind you. `@Disabled` with a reason
at least leaves a trace.

## The helpers

| | |
|---|---|
| `Await.until(description, condition)` | poll to a deadline, fail with a clear message |
| `Concurrently.run(threads, iters, task)` | start gate, so the operations actually overlap |
| `Concurrently.collect(threads, supplier)` | one result per thread, asserted on the test thread |

> Source: `src/test/java/org/alxkm/testsupport/`
