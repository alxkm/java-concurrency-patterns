# Contributing

Corrections are worth more here than additions. If an example is wrong, a test passes against broken
code, or a number in the README does not reproduce, that is the most useful issue or pull request you can
send.

## What this repository asks of every claim

Each example demonstrates one mechanism, and each claim about it is backed by something that runs:

| The claim | What backs it |
|---|---|
| "this is thread safe", "this loses updates" | a JUnit test in `src/test` |
| "this is faster" | a JMH benchmark in `src/jmh`, with the numbers quoted from a run |
| "this reordering can happen", "this one cannot" | a jcstress test in `src/jcstress` |

A pull request that adds a claim without the thing that backs it will be asked for one.

## Running things

JDK 21 or newer. The Gradle wrapper is checked in.

```bash
./gradlew build                                        # compile everything, run the tests
./gradlew listExamples                                 # every example with a main method
./gradlew runExample -Pexample=VisibilityExample       # run one

./gradlew jcstress -PjcstressArgs="-t PlainFields -m quick"   # one stress test
./gradlew jmh -PjmhArgs="Counter -f 1 -wi 3 -i 3"             # one benchmark
```

`build` compiles the jcstress and JMH source sets but does not run them: a real run takes minutes.

## Writing a test for concurrent code

The tests here assert the concurrency property in question, and only what is guaranteed:

- **Assert invariants, not the outcome of a race.** "The unguarded counter never overcounts" always
  holds. "The unguarded counter loses updates" usually holds, which makes it a flaky test. If the
  interesting behaviour is not guaranteed, print it from the example, or show it under jcstress.
- **Never let `Thread.sleep` stand in for a condition.** Wait for the condition with
  `testsupport.Await.until(...)`, and bound every wait so a hang fails instead of stalling the build.
- **Make threads actually overlap.** Threads started in a loop often run one after another.
  `testsupport.Concurrently.run(...)` releases them all through a start gate.
- **Check the test against the broken version.** A test that also passes against the antipattern proves
  nothing about the fix.
- **Do not comment a test out.** Fix it, or disable it with `@Disabled("why")`. `RepositoryHygieneTest`
  fails the build on a commented-out test and on a `@Disabled` without a reason.

Reordering cannot be caught by a unit test at all: whatever lines the two threads up is itself a memory
barrier. That is what the jcstress source set is for.

## The shape of an example

- A small class in the package of its topic, mirroring the existing layout. One mechanism per class.
- A Javadoc comment that explains the mechanism, and when not to use it. For an antipattern: why it
  breaks, next to the version that fixes it.
- Logic in a method that returns what it did, and a `main` that prints it. A test can call the method,
  and `runExample` can run the `main`.
- A link in the matching README section. The counts near the top of the README are of files, so update
  them when you add one.

## Commits and pull requests

Commit subjects follow the prefixes already in the history (`feat:`, `fix:`, `test:`, `docs:`,
`build:`, `ci:`), written in the imperative and in lower case. The body says why, not what: the diff
already shows what.

Plain English throughout: code, comments, commit messages and docs.
