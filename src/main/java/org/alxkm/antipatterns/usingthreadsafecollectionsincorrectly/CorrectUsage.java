package org.alxkm.antipatterns.usingthreadsafecollectionsincorrectly;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * To resolve the race in {@link IncorrectUsage}, the check and the add have to happen as one
 * indivisible step.
 * <p>
 * {@link CopyOnWriteArrayList} already offers exactly that step as
 * {@link CopyOnWriteArrayList#addIfAbsent(Object)}, which performs the contains-then-add atomically
 * under the list's own lock. Prefer it over an external {@code synchronized} block: hand-rolling the
 * guard means paying for the copy-on-write array copy <em>and</em> for the lock, and it only works
 * as long as every caller remembers to go through the guarded method.
 * <p>
 * Note that {@link #size()} and {@link #getCollection()} need no synchronization here, because the
 * underlying collection is itself thread-safe. That is the part {@code synchronized} on a single
 * mutator would not have given us for free.
 *
 * @see OptimizedUsage for the variant that avoids the O(n) copy per insert.
 */
public class CorrectUsage implements BaseListUsage<String> {
    private final CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

    /**
     * Adds a new element to the list if it's not already present.
     * <p>
     * The atomicity comes from the collection, not from the caller.
     *
     * @param element the element to add to the list.
     */
    @Override
    public void addIfAbsent(String element) {
        list.addIfAbsent(element);
    }

    /**
     * Returns the size of the list.
     *
     * @return the number of elements in the list.
     */
    @Override
    public int size() {
        return list.size();
    }

    @Override
    public List<String> getCollection() {
        return list;
    }
}
