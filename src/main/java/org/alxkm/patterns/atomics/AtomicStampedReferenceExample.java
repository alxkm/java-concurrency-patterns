package org.alxkm.patterns.atomics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * AtomicStampedReference and the ABA problem it exists to solve.
 * <p>
 * A compare-and-set asks "is the value still what I read?". It cannot ask "has anything happened
 * since I read it?". If another thread changes the value from A to B and back to A in between, the
 * CAS succeeds as if nothing happened. That is the ABA problem, and it matters when the decision a
 * thread made after reading A depends on more than A itself.
 * <p>
 * The classic case is a lock-free stack. pop() reads the top node A and its successor B, then CASes
 * the top from A to B. If, in between, another thread pops A, pops B and pushes A back, the top is A
 * again and the CAS succeeds, but it installs B, a node that is no longer on the stack. B now has two
 * owners: the thread that popped it and the stack.
 * <p>
 * In Java this needs nodes to be reused, as an object pool or a free list does. With a fresh node per
 * push, the paused thread's reference keeps A alive, so no other node can ever be the same A. That is
 * why ABA is rare in Java and common in C, but pools exist, and so does the problem.
 * <p>
 * AtomicStampedReference pairs the reference with an int stamp that every update increments. The
 * paused thread CASes on reference and stamp together, so A-B-A is caught: the reference is the same,
 * the stamp is not.
 */
public final class AtomicStampedReferenceExample {

    private AtomicStampedReferenceExample() {
    }

    /**
     * A stack node. Reusable: push() rewrites next, so the same node can go back on a stack after
     * being popped.
     */
    public static final class Node {
        private final String value;
        private Node next;

        public Node(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * A lock-free stack.
     * <p>
     * pop(Runnable) runs the given action once, after reading the top and before the CAS. It stands for
     * another thread that runs while this one is paused at the worst moment. With real threads the
     * scheduler pauses there only rarely; calling the action inline makes the interleaving certain.
     */
    public interface Stack {
        void push(Node node);

        Node pop(Runnable betweenReadAndCas);

        default Node pop() {
            return pop(() -> { });
        }

        /**
         * @return the values from top to bottom. Not atomic, for checking a stack no one is changing.
         */
        List<String> values();
    }

    /**
     * Treiber stack on a plain AtomicReference. Correct with fresh nodes, broken by ABA with reused ones.
     */
    public static final class PlainStack implements Stack {
        private final AtomicReference<Node> top = new AtomicReference<>();

        @Override
        public void push(Node node) {
            while (true) {
                Node current = top.get();
                node.next = current;
                if (top.compareAndSet(current, node)) {
                    return;
                }
            }
        }

        @Override
        public Node pop(Runnable betweenReadAndCas) {
            Runnable pause = betweenReadAndCas;
            while (true) {
                Node current = top.get();
                if (current == null) {
                    return null;
                }
                Node next = current.next;
                pause.run();
                pause = () -> { };
                // Only asks whether the top is still the same node, not whether the stack is the same.
                if (top.compareAndSet(current, next)) {
                    return current;
                }
            }
        }

        @Override
        public List<String> values() {
            return valuesFrom(top.get());
        }
    }

    /**
     * The same stack on an AtomicStampedReference. Every push and pop increments the stamp, so a CAS
     * from a thread that read an older stamp fails, and it retries with what is there now.
     * <p>
     * Bumping on push alone, or on pop alone, would also catch ABA here, because A can only get back to
     * the top by being popped and pushed again. Bumping on every update is the rule that needs no such
     * argument.
     */
    public static final class StampedStack implements Stack {
        private final AtomicStampedReference<Node> top = new AtomicStampedReference<>(null, 0);

        @Override
        public void push(Node node) {
            int[] stamp = new int[1];
            while (true) {
                Node current = top.get(stamp);
                node.next = current;
                if (top.compareAndSet(current, node, stamp[0], stamp[0] + 1)) {
                    return;
                }
            }
        }

        @Override
        public Node pop(Runnable betweenReadAndCas) {
            Runnable pause = betweenReadAndCas;
            int[] stamp = new int[1];
            while (true) {
                // Reads the reference and the stamp together, as one atomic snapshot.
                Node current = top.get(stamp);
                if (current == null) {
                    return null;
                }
                Node next = current.next;
                pause.run();
                pause = () -> { };
                if (top.compareAndSet(current, next, stamp[0], stamp[0] + 1)) {
                    return current;
                }
            }
        }

        @Override
        public List<String> values() {
            return valuesFrom(top.getReference());
        }
    }

    /**
     * Builds the stack A, B, C (A on top) and pops it while another thread pops A, pops B, and pushes A
     * back. After that the stack should hold only C.
     *
     * @param stack an empty stack.
     * @return what the stack holds afterwards, top first.
     */
    public static List<String> abaScenario(Stack stack) {
        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");
        stack.push(c);
        stack.push(b);
        stack.push(a);

        // This pop reads top = A and next = B, then pauses while the other thread works.
        stack.pop(() -> {
            Node popped = stack.pop();
            stack.pop();
            stack.push(popped); // A goes back on top, now pointing at C
        });
        return stack.values();
    }

    private static List<String> valuesFrom(Node top) {
        List<String> values = new ArrayList<>();
        for (Node node = top; node != null; node = node.next) {
            values.add(node.value);
        }
        return values;
    }

    public static void main(String[] args) {
        System.out.println("Stack A, B, C. One pop is paused after reading A and B; meanwhile another thread");
        System.out.println("pops A, pops B and pushes A back. Afterwards the stack should be [C].");
        System.out.println("AtomicReference:        " + abaScenario(new PlainStack())
                + "  <- B is back, though another thread popped it");
        System.out.println("AtomicStampedReference: " + abaScenario(new StampedStack())
                + "  <- the stamp changed, so the CAS failed and the pop retried");
    }
}
