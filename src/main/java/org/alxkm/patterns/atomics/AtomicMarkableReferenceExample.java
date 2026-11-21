package org.alxkm.patterns.atomics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AtomicMarkableReference and the lock-free linked list it is made for.
 * <p>
 * A lock-free sorted list removes a node by swinging its predecessor's next past it, with one CAS.
 * That CAS checks the predecessor's link and nothing else, and the node being removed can change
 * meanwhile. Two things go wrong, both shown below:
 * <ul>
 *   <li><b>A lost insert.</b> remove(20) reads 10, 20, 30. Before it swings 10.next to 30, another
 *   thread inserts 25 after 20. The CAS on 10.next still succeeds and unlinks 20, and 25 with it.
 *   add(25) returned true, and 25 is not in the list.</li>
 *   <li><b>A removed node comes back.</b> remove(20) reads 10, 20, 30 and pauses; another thread
 *   removes 30 by swinging 20.next past it. Then 10.next is set to 30, and 30 is back.</li>
 * </ul>
 * Both have the same cause: the thread unlinked a node whose own next it did not control. The fix
 * (Harris, 2001) is to freeze a node's next before unlinking it. Each next is an
 * AtomicMarkableReference, and remove() first sets the mark on the victim's next, with a CAS that
 * expects the successor it read and no mark. From then on the node is deleted, logically, and every
 * CAS that expects its next unmarked fails, so nobody can insert after it or unlink its successor
 * through it. Only then is it unlinked, physically, and any thread that finds a marked node on its
 * way helps unlink it.
 * <p>
 * The reference and the mark have to change in one CAS. A separate volatile boolean would leave a gap
 * between checking the flag and swinging the link, which is the same race again.
 */
public final class AtomicMarkableReferenceExample {

    private AtomicMarkableReferenceExample() {
    }

    /**
     * A set of ints kept as a sorted linked list.
     * <p>
     * remove(int, Runnable) runs the given action once, after finding the node and reading its successor,
     * and before changing anything. It stands for another thread that runs while this one is paused at the
     * worst moment; calling it inline makes the interleaving certain instead of rare.
     */
    public interface SortedIntSet {
        /**
         * @param key a value strictly between Integer.MIN_VALUE and Integer.MAX_VALUE, which the list
         *            keeps for its head and tail.
         * @return true if the key was added, false if it was there already.
         */
        boolean add(int key);

        boolean remove(int key, Runnable afterReadingSuccessor);

        default boolean remove(int key) {
            return remove(key, () -> { });
        }

        boolean contains(int key);

        /**
         * @return the keys in order. Not atomic, for checking a set no one is changing.
         */
        List<Integer> values();
    }

    /**
     * The naive lock-free list: one AtomicReference per link, removal by one CAS on the predecessor.
     * add() alone is correct; add() and remove() together, or two remove()s of neighbours, are not.
     */
    public static final class PlainList implements SortedIntSet {
        private static final class Node {
            final int key;
            final AtomicReference<Node> next;

            Node(int key, Node next) {
                this.key = key;
                this.next = new AtomicReference<>(next);
            }
        }

        private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

        @Override
        public boolean add(int key) {
            checkKey(key);
            while (true) {
                Node pred = head;
                Node curr = pred.next.get();
                while (curr.key < key) {
                    pred = curr;
                    curr = curr.next.get();
                }
                if (curr.key == key) {
                    return false;
                }
                if (pred.next.compareAndSet(curr, new Node(key, curr))) {
                    return true;
                }
            }
        }

        @Override
        public boolean remove(int key, Runnable afterReadingSuccessor) {
            checkKey(key);
            Runnable pause = afterReadingSuccessor;
            while (true) {
                Node pred = head;
                Node curr = pred.next.get();
                while (curr.key < key) {
                    pred = curr;
                    curr = curr.next.get();
                }
                if (curr.key != key) {
                    return false;
                }
                Node succ = curr.next.get();
                pause.run();
                pause = () -> { };
                // Checks that pred still points at curr. Says nothing about curr.next, which may have changed.
                if (pred.next.compareAndSet(curr, succ)) {
                    return true;
                }
            }
        }

        @Override
        public boolean contains(int key) {
            checkKey(key);
            Node curr = head.next.get();
            while (curr.key < key) {
                curr = curr.next.get();
            }
            return curr.key == key;
        }

        @Override
        public List<Integer> values() {
            List<Integer> values = new ArrayList<>();
            for (Node node = head.next.get(); node.key != Integer.MAX_VALUE; node = node.next.get()) {
                values.add(node.key);
            }
            return values;
        }
    }

    /**
     * The Harris-Michael list: each link is an AtomicMarkableReference, and a set mark on a node's next
     * means the node is deleted. Lock-free: some thread always makes progress. contains() is wait-free:
     * it never retries and never writes.
     */
    public static final class MarkedList implements SortedIntSet {
        private static final class Node {
            final int key;
            final AtomicMarkableReference<Node> next;

            Node(int key, Node next) {
                this.key = key;
                this.next = new AtomicMarkableReference<>(next, false);
            }
        }

        /**
         * Two neighbours with pred.key &lt; key &lt;= curr.key, found linked and unmarked.
         */
        private record Window(Node pred, Node curr) {
        }

        private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

        /**
         * Finds the window for a key, unlinking every marked node on the way. A node that is marked but
         * still linked is one whose remover has not done the second step yet, or failed it; any thread
         * may finish the job. If that CAS fails, pred changed under us, and the search starts over.
         */
        private Window find(int key) {
            boolean[] marked = new boolean[1];
            retry:
            while (true) {
                Node pred = head;
                Node curr = pred.next.getReference();
                while (true) {
                    Node succ = curr.next.get(marked);
                    while (marked[0]) {
                        if (!pred.next.compareAndSet(curr, succ, false, false)) {
                            continue retry;
                        }
                        curr = succ;
                        succ = curr.next.get(marked);
                    }
                    if (curr.key >= key) {
                        return new Window(pred, curr);
                    }
                    pred = curr;
                    curr = succ;
                }
            }
        }

        @Override
        public boolean add(int key) {
            checkKey(key);
            while (true) {
                Window window = find(key);
                if (window.curr.key == key) {
                    return false;
                }
                // Fails if pred was marked meanwhile: nothing may be inserted after a deleted node.
                if (window.pred.next.compareAndSet(window.curr, new Node(key, window.curr), false, false)) {
                    return true;
                }
            }
        }

        @Override
        public boolean remove(int key, Runnable afterReadingSuccessor) {
            checkKey(key);
            Runnable pause = afterReadingSuccessor;
            while (true) {
                Window window = find(key);
                Node curr = window.curr;
                if (curr.key != key) {
                    return false;
                }
                Node succ = curr.next.getReference();
                pause.run();
                pause = () -> { };
                // Step one, the logical delete: mark curr.next, but only if it is still succ and unmarked.
                // If someone inserted after curr, or unlinked succ, or deleted curr first, this fails and
                // the search starts over.
                if (curr.next.compareAndSet(succ, succ, false, true)) {
                    // Step two, the physical unlink. If it fails, a later find() does it, so it is not retried.
                    window.pred.next.compareAndSet(curr, succ, false, false);
                    return true;
                }
            }
        }

        @Override
        public boolean contains(int key) {
            checkKey(key);
            Node curr = head.next.getReference();
            while (curr.key < key) {
                curr = curr.next.getReference();
            }
            return curr.key == key && !curr.next.isMarked();
        }

        @Override
        public List<Integer> values() {
            List<Integer> values = new ArrayList<>();
            for (Node node = head.next.getReference(); node.key != Integer.MAX_VALUE;
                 node = node.next.getReference()) {
                if (!node.next.isMarked()) {
                    values.add(node.key);
                }
            }
            return values;
        }
    }

    /**
     * Fills the set with 10, 20, 30 and removes 20, while another thread inserts 25 between remove's
     * read of 20's successor and its unlink. Afterwards the set should be 10, 25, 30.
     *
     * @param set an empty set.
     * @return the set's keys afterwards.
     */
    public static List<Integer> insertDuringRemove(SortedIntSet set) {
        set.add(10);
        set.add(20);
        set.add(30);
        set.remove(20, () -> set.add(25));
        return set.values();
    }

    /**
     * Fills the set with 10, 20, 30 and removes 20, while another thread removes 30 at the same point.
     * Afterwards the set should be 10.
     *
     * @param set an empty set.
     * @return the set's keys afterwards.
     */
    public static List<Integer> removeNeighbourDuringRemove(SortedIntSet set) {
        set.add(10);
        set.add(20);
        set.add(30);
        set.remove(20, () -> set.remove(30));
        return set.values();
    }

    private static void checkKey(int key) {
        if (key == Integer.MIN_VALUE || key == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("reserved for the head and tail: " + key);
        }
    }

    public static void main(String[] args) {
        System.out.println("List 10, 20, 30. remove(20) is paused after reading 20's successor, 30.");
        System.out.println("Meanwhile add(25); afterwards the list should be [10, 25, 30].");
        System.out.println("  AtomicReference:         " + insertDuringRemove(new PlainList())
                + "  <- 25 went out with 20");
        System.out.println("  AtomicMarkableReference: " + insertDuringRemove(new MarkedList())
                + "  <- marking 20 failed, so remove retried");
        System.out.println("Meanwhile remove(30); afterwards the list should be [10].");
        System.out.println("  AtomicReference:         " + removeNeighbourDuringRemove(new PlainList())
                + "  <- 10.next was set to 30, which was already removed");
        System.out.println("  AtomicMarkableReference: " + removeNeighbourDuringRemove(new MarkedList()));
    }
}
