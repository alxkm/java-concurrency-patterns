package org.alxkm.patterns.immutable;

/**
 * The Immutable class ensures objects are immutable,
 * meaning their state cannot be modified after construction.
 * This makes them safe to share between threads without synchronization.
 */
public final class Immutable {
    private final int value;

    /**
     * Constructs an Immutable object with the specified value.
     *
     * @param value the value of the Immutable object
     */
    public Immutable(int value) {
        this.value = value;
    }

    /**
     * Retrieves the value of the Immutable object.
     *
     * @return the value of the Immutable object
     */
    public int getValue() {
        return value;
    }
}