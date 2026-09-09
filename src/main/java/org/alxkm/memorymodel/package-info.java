/**
 * The Java Memory Model: what the other examples in this repository quietly depend on.
 * <p>
 * Every other package here shows a mechanism -- a lock, a queue, an atomic. This one shows the rules
 * those mechanisms exist to satisfy. Without them, advice like "double-checked locking needs volatile"
 * is a recipe to memorise rather than something you can reason about.
 * <p>
 * The model is defined in terms of <em>happens-before</em>: an ordering between actions in different
 * threads. If a write happens-before a read, the read must see that write. If no such edge exists, the
 * read may see the write, or may see a stale value, or may see the actions in a different order than
 * the source code lists them -- and the compiler, JIT and CPU are all free to exploit that freedom.
 * <p>
 * The classes here are grouped by what each one demonstrates:
 * <ul>
 *   <li>{@link org.alxkm.memorymodel.VisibilityExample} -- a write that another thread never sees, and
 *       the one-word change that fixes it. The clearest demonstration in this package: it reproduces on
 *       every run.</li>
 *   <li>{@link org.alxkm.memorymodel.HappensBeforeExample} -- the four edges you get for free from
 *       {@code Thread.start()}, {@code Thread.join()}, a volatile write/read pair, and a lock.</li>
 *   <li>{@link org.alxkm.memorymodel.SafePublicationExample} -- how to hand a newly built object to
 *       another thread so it cannot observe it half-constructed.</li>
 *   <li>{@link org.alxkm.memorymodel.FalseSharingExample} -- correctness is not the only thing the
 *       memory model costs you; two unrelated fields on one cache line are measurably slower.</li>
 * </ul>
 *
 * <h2>Why some of this is not a plain unit test</h2>
 * Visibility and false sharing reproduce reliably, so they are demonstrated here directly. Instruction
 * reordering does not: a hand-written test needs some synchronisation to line the two threads up, and
 * that synchronisation is itself a memory barrier that hides the very effect being looked for. Measured
 * on this repository, the textbook Dekker probe found zero reorderings in 20,000 thread-pair runs and
 * zero in 500,000 barrier-synchronised iterations.
 * <p>
 * That is what {@code jcstress} is for. It is the OpenJDK harness built to expose exactly these races:
 * it spins the actors without synchronisation, samples in bulk, and shuffles JIT decisions between forks.
 * Given the same Dekker idiom it found the reordering in about 4% of 265 million samples. The tests live
 * in the {@code src/jcstress} source set; run them with {@code ./gradlew jcstress} (a few minutes).
 * <p>
 * That gap -- zero in 520,000 hand-rolled attempts, tens of millions under jcstress -- is itself the
 * point. These bugs do not fail loudly in testing. They fail in production, rarely, on someone else's
 * hardware.
 */
package org.alxkm.memorymodel;
