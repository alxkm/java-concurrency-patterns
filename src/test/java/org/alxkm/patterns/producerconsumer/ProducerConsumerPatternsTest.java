package org.alxkm.patterns.producerconsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the five producer-consumer variations.
 * <p>
 * The previous version of this class built its own queues and its own producer and consumer loops
 * inline, so it exercised java.util.concurrent rather than anything in this package: coverage of these
 * classes sat at 5%. Each example now exposes a run method that returns what it moved through the
 * queue, so the tests call the examples themselves.
 * <p>
 * Conservation is the property every variation shares: nothing is lost, nothing is delivered twice.
 * Beyond that each one is tested for the thing that makes it different, which is the reason to pick it
 * over the others.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class ProducerConsumerPatternsTest {

    /**
     * The bounded queue holds 10, so the last case pushes far more through it than fits at once. That
     * is the case worth covering: producers block on a full queue and resume without dropping work.
     */
    @ParameterizedTest(name = "{0} producers, {1} consumers, {2} items each")
    @CsvSource({"1, 1, 5", "3, 1, 4", "1, 3, 6", "2, 2, 25"})
    void basicConservesItems(int producers, int consumers, int itemsPerProducer)
            throws InterruptedException {
        BasicProducerConsumerExample.Result result =
                BasicProducerConsumerExample.run(producers, consumers, itemsPerProducer);

        assertEquals(producers * itemsPerProducer, result.produced());
        assertEquals(producers * itemsPerProducer, result.consumed());
    }

    @Test
    void batchConservesItemsAndGroupsThemIntoBatches() throws InterruptedException {
        int producers = 3;
        int itemsPerProducer = 10;
        int batchSize = 5;

        BatchProducerConsumerExample.Result result =
                BatchProducerConsumerExample.run(producers, 2, itemsPerProducer, batchSize);

        int total = producers * itemsPerProducer;
        assertEquals(total, result.produced());
        assertEquals(total, result.consumed());

        // Batching is the point: the consumers must have grouped the work rather than taking it one at
        // a time. A partial batch can be flushed on timeout, so the count is a range, not an equality.
        assertTrue(result.batches() >= total / batchSize,
                "expected at least " + (total / batchSize) + " batches, got " + result.batches());
        assertTrue(result.batches() < total,
                "items should be grouped, but " + result.batches() + " batches for " + total + " items"
                        + " means they were processed nearly one by one");
    }

    @Test
    void priorityConservesItems() throws InterruptedException {
        PriorityProducerConsumerExample.Result result =
                PriorityProducerConsumerExample.run(2, 2, 10);

        assertEquals(20, result.produced());
        assertEquals(20, result.consumed());
    }

    /**
     * Priority ordering is a property of each take, not of a run. While producers are still publishing,
     * a consumer can only take the highest priority item present at that moment, so a concurrent run
     * gives no sorted sequence to assert on. Draining a preloaded queue does.
     */
    @Test
    void priorityQueueHandsOutTheHighestPriorityFirst() {
        List<Integer> order = PriorityProducerConsumerExample.drainInPriorityOrder(50);

        assertEquals(50, order.size());
        for (int i = 1; i < order.size(); i++) {
            assertTrue(order.get(i - 1) >= order.get(i),
                    "priorities should come out non-increasing, but got " + order);
        }
    }

    @Test
    void delayedConservesTasksAndNeverDeliversEarly() throws InterruptedException {
        DelayedProducerConsumerExample.Result result =
                DelayedProducerConsumerExample.run(2, 2, 10, 200);

        assertEquals(20, result.produced());
        assertEquals(20, result.consumed());
        assertEquals(0, result.earlyDeliveries(),
                "a DelayQueue must not hand out a task before its delay has elapsed");
    }

    @Test
    void transferConservesItems() throws InterruptedException {
        TransferQueueExample.Result result = TransferQueueExample.run(2, 2, 5);

        assertEquals(10, result.produced());
        assertEquals(10, result.consumed());
    }

    /**
     * What a TransferQueue adds over an ordinary queue: the producer learns the work was picked up,
     * not merely queued.
     */
    @Test
    void transferBlocksUntilAConsumerTakesTheItem() throws InterruptedException {
        assertTrue(TransferQueueExample.transferWaitsForAConsumer(200),
                "transfer() should stay blocked while the item is unclaimed, then return once taken");
    }
}
