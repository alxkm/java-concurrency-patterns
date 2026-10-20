## What this changes and why

<!-- A new example, a correction, a test that now catches something, a benchmark. -->

## Checklist

- [ ] `./gradlew build` passes
- [ ] Every new claim is backed by something that runs: a test, a JMH benchmark or a jcstress test
- [ ] New tests assert only what is guaranteed, not the outcome of a race
- [ ] A new test fails against the broken version of the code, not only passes against the fixed one
- [ ] No `Thread.sleep` standing in for a condition, and every wait has a timeout
- [ ] A new example is linked from the matching README section, and the counts near the top are updated

<!-- For a performance claim, paste the JMH output and the machine it ran on (cores, OS, JDK). -->
