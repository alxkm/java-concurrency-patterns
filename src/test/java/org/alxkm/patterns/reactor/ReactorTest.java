package org.alxkm.patterns.reactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.Socket;

public class ReactorTest {

    /**
     * This test method verifies the functionality of the Reactor class, which implements a simple reactor
     * pattern for handling incoming socket connections. It starts a Reactor instance on a separate thread,
     * then creates a socket and attempts to connect to the localhost on the specified port (12345). The test
     * asserts that the socket connection is successful by checking if the socket is connected. After the
     * connection is established, the socket is closed, and then the Reactor is stopped and the reactorThread
     * is joined to ensure proper termination.
     */
    @Test
    public void testReactor() throws IOException, InterruptedException {
        Reactor reactor = new Reactor(12345);
        Thread reactorThread = new Thread(reactor);
        reactorThread.start();

        Socket socket = new Socket("localhost", 12345);
        assertTrue(socket.isConnected());
        socket.close();

        reactor.stop();
        reactorThread.join();
    }
}
