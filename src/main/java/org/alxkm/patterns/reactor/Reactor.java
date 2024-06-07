package org.alxkm.patterns.reactor;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Handles service requests that are delivered concurrently to an application by one or more inputs.
 * The Reactor class implements a reactor pattern, allowing it to efficiently handle multiple
 * concurrent requests through non-blocking I/O operations.
 */
public class Reactor implements Runnable {
    private final Selector selector; // The selector for handling I/O events
    private final ServerSocketChannel serverSocketChannel; // The server socket channel for accepting incoming connections

    /**
     * Constructs a new Reactor instance that binds to the specified port.
     *
     * @param port The port to bind the server socket channel to.
     * @throws IOException If an I/O error occurs while opening or configuring the server socket channel.
     */
    public Reactor(int port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new java.net.InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Main execution loop of the Reactor, continuously handles incoming I/O events.
     * This method should be invoked by starting a new thread.
     */
    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        if (client != null) {
                            // Handle new connection
                        }
                    }
                }
            }
        } catch (IOException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the Reactor by closing the selector and server socket channel.
     * This method should be invoked to gracefully shutdown the Reactor.
     *
     * @throws IOException If an I/O error occurs while closing the selector or server socket channel.
     */
    public void stop() throws IOException {
        selector.close();
        serverSocketChannel.close();
    }
}
