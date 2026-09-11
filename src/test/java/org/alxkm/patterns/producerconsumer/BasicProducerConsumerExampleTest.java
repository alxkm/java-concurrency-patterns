package org.alxkm.patterns.producerconsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link BasicProducerConsumerExample}.
 *
 * The property that matters for a producer-consumer queue is conservation: nothing is lost and nothing
 * is delivered twice, whatever the ratio of producers to consumers. The bounded queue makes producers
 * block when it fills and consumers block when it drains, and neither should cost an item.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class BasicProducerConsumerExampleTest {

    @Test
    void everyProducedItemIsConsumedExactlyOnce() throws InterruptedException {
        BasicProducerConsumerExample.Result result = BasicProducerConsumerExample.run(2, 3, 5);

        assertEquals(10, result.produced());
        assertEquals(result.produced(), result.consumed(), "no item should be lost or double counted");
    }

    /**
     * The queue holds 10, so the last case pushes far more through it than it can hold at once. That is
     * the case worth covering: producers have to block on a full queue and resume without dropping work.
     */
    @ParameterizedTest(name = "{0} producers, {1} consumers, {2} items each")
    @CsvSource({
            "1, 1, 5",
            "3, 1, 4",
            "1, 3, 6",
            "2, 2, 25"
    })
    void conservesItemsAcrossProducerConsumerRatios(int producers, int consumers, int itemsPerProducer)
            throws InterruptedException {
        BasicProducerConsumerExample.Result result =
                BasicProducerConsumerExample.run(producers, consumers, itemsPerProducer);

        assertEquals(producers * itemsPerProducer, result.produced());
        assertEquals(producers * itemsPerProducer, result.consumed());
    }
}
