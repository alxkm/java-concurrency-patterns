package org.alxkm.patterns.forkjoinpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ForkJoinMergeSortTest {

    /**
     * Sorting is checked against {@link Arrays#sort(int[])} across sizes that straddle the
     * sequential threshold, so both the direct and the forked paths are exercised.
     */
    @ParameterizedTest(name = "sorts {0} random elements")
    @ValueSource(ints = {0, 1, 2, 3, 7, 100, 8_191, 8_192, 8_193, 100_000})
    void sortsRandomArraysLikeArraysSort(int size) {
        int[] actual = new Random(size).ints(size, -1_000, 1_000).toArray();
        int[] expected = actual.clone();
        Arrays.sort(expected);

        ForkJoinMergeSort.sort(actual);

        assertArrayEquals(expected, actual);
    }

    /**
     * The inputs a merge sort is most likely to get wrong: already ordered, exactly reversed, all
     * equal, and containing the extreme int values.
     */
    @Test
    void sortsDegenerateInputs() {
        assertSorted(new int[]{1, 2, 3, 4, 5});
        assertSorted(new int[]{5, 4, 3, 2, 1});
        assertSorted(new int[]{7, 7, 7, 7});
        assertSorted(new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1});
    }

    private static void assertSorted(int[] input) {
        int[] expected = input.clone();
        Arrays.sort(expected);

        int[] actual = input.clone();
        ForkJoinMergeSort.sort(actual);

        assertArrayEquals(expected, actual, "failed for " + Arrays.toString(input));
    }
}
