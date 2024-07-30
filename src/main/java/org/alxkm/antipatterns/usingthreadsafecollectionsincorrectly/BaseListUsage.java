package org.alxkm.antipatterns.usingthreadsafecollectionsincorrectly;

import java.util.Collection;

public interface BaseListUsage<T> {
    void addIfAbsent(String element);

    int size();

    Collection<T> getCollection();
}
