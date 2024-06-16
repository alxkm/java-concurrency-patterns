package org.alxkm.patterns.monitorobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonitorObjectTest {

    /**
     * This test method verifies the behavior of the MonitorObject class, which demonstrates
     * the use of synchronization and mutual exclusion to protect shared resources in a multi-threaded
     * environment. It creates two threads, t1 and t2, both of which invoke the increment method
     * of the MonitorObject instance concurrently. The test ensures that both threads complete their
     * increments before checking the final count value. The expected count value after both threads
     * complete their increments is 2, indicating that the increment operation is properly synchronized
     * and the shared resource (count) is updated correctly by multiple threads.
     */
    @Test
    public void testMonitorObject() throws InterruptedException {
        MonitorObject monitorObject = new MonitorObject();
        Thread t1 = new Thread(monitorObject::increment);
        Thread t2 = new Thread(monitorObject::increment);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertEquals(2, monitorObject.getCount());
    }
}
