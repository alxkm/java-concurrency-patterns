package org.alxkm.patterns.forkjoinpool;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * The ForkJoinMergeSort class demonstrates the usage of the ForkJoinPool framework
 * to perform parallel merge sort on an array of integers.
 */
public class ForkJoinMergeSort {

    /**
     * The main method initializes an array of integers, creates a ForkJoinPool,
     * and invokes the MergeSortTask to perform parallel merge sort on the array.
     * It then prints the sorted array.
     *
     * @param args The command-line arguments (unused).
     */
    public static void main(String[] args) {
        int[] array = {38, 27, 43, 3, 9, 82, 10};
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new MergeSortTask(array, 0, array.length - 1));

        for (int i : array) {
            System.out.print(i + " ");
        }
    }

    /**
     * The MergeSortTask class extends RecursiveAction and represents a task
     * that performs merge sort on a portion of the array in parallel.
     */
    static class MergeSortTask extends RecursiveAction {
        private int[] array;
        private int left;
        private int right;

        /**
         * Constructs a MergeSortTask with the given array and the range to sort.
         *
         * @param array The array to be sorted.
         * @param left  The left index of the range to sort.
         * @param right The right index of the range to sort.
         */
        MergeSortTask(int[] array, int left, int right) {
            this.array = array;
            this.left = left;
            this.right = right;
        }

        /**
         * The compute method performs the merge sort operation recursively.
         * It divides the array into two halves, creates subtasks for each half,
         * and merges the sorted halves.
         */
        @Override
        protected void compute() {
            if (left < right) {
                int mid = (left + right) / 2;

                MergeSortTask leftTask = new MergeSortTask(array, left, mid);
                MergeSortTask rightTask = new MergeSortTask(array, mid + 1, right);

                invokeAll(leftTask, rightTask);

                merge(array, left, mid, right);
            }
        }

        /**
         * Merges two sorted halves of the array.
         *
         * @param array The array to be merged.
         * @param left  The left index of the first half.
         * @param mid   The middle index.
         * @param right The right index of the second half.
         */
        private void merge(int[] array, int left, int mid, int right) {
            int n1 = mid - left + 1;
            int n2 = right - mid;

            int[] L = new int[n1];
            int[] R = new int[n2];

            System.arraycopy(array, left, L, 0, n1);
            System.arraycopy(array, mid + 1, R, 0, n2);

            int i = 0, j = 0, k = left;
            while (i < n1 && j < n2) {
                if (L[i] <= R[j]) {
                    array[k] = L[i];
                    i++;
                } else {
                    array[k] = R[j];
                    j++;
                }
                k++;
            }

            while (i < n1) {
                array[k] = L[i];
                i++;
                k++;
            }

            while (j < n2) {
                array[k] = R[j];
                j++;
                k++;
            }
        }
    }
}