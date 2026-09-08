package org.alxkm.patterns.reactor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorTest {

    /**
     * Verifies the full dispatch path: the reactor accepts a connection, registers it for reads and
     * hands the bytes that arrive to its handler.
     * <p>
     * Asserting only that {@link Socket#isConnected()} returns true would prove nothing, because the
     * OS completes the TCP handshake from the listen backlog whether or not the reactor ever calls
     * accept().
     */
    @Test
    void dispatchesReceivedDataToTheHandler() throws IOException, InterruptedException {
        LinkedBlockingQueue<String> received = new LinkedBlockingQueue<>();
        EventHandler handler = (channel, data) -> {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            received.add(new String(bytes, StandardCharsets.UTF_8));
        };

        // Port 0 lets the OS pick a free port, so the test cannot collide with a busy one.
        Reactor reactor = new Reactor(0, handler);
        Thread reactorThread = new Thread(reactor, "reactor");
        reactorThread.start();

        try {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", reactor.getPort()), 5_000);
                OutputStream out = socket.getOutputStream();
                out.write("ping".getBytes(StandardCharsets.UTF_8));
                out.flush();

                assertEquals("ping", received.poll(5, TimeUnit.SECONDS),
                        "the reactor should have dispatched the payload to its handler");
            }
        } finally {
            reactor.stop();
            reactorThread.join(TimeUnit.SECONDS.toMillis(5));
        }

        assertFalse(reactorThread.isAlive(), "stop() should let the dispatch loop return");
    }

    /**
     * A reactor serves many connections from its single thread; none of them gets a thread of its
     * own, and none of them starves the others.
     */
    @Test
    void servesMultipleConnectionsFromOneThread() throws IOException, InterruptedException {
        int connections = 5;
        CountDownLatch allReceived = new CountDownLatch(connections);
        EventHandler handler = (channel, data) -> allReceived.countDown();

        Reactor reactor = new Reactor(0, handler);
        Thread reactorThread = new Thread(reactor, "reactor");
        reactorThread.start();

        try {
            for (int i = 0; i < connections; i++) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", reactor.getPort()), 5_000);
                    socket.getOutputStream().write(('a' + i));
                    socket.getOutputStream().flush();
                }
            }

            assertTrue(allReceived.await(10, TimeUnit.SECONDS),
                    "expected all " + connections + " payloads, still missing " + allReceived.getCount());
        } finally {
            reactor.stop();
            reactorThread.join(TimeUnit.SECONDS.toMillis(5));
        }
    }
}
