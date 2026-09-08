package org.alxkm.patterns.reactor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Handles service requests delivered concurrently to an application by one or more inputs.
 * <p>
 * A single thread blocks in {@link Selector#select()}, and each time one or more channels become
 * ready it demultiplexes the readiness events and dispatches them: new connections are accepted and
 * registered for reads, and readable connections are drained and passed to an {@link EventHandler}.
 * That is the whole pattern -- one thread serving many connections, with no thread per connection
 * and no blocking I/O.
 */
public class Reactor implements Runnable {
    private static final int READ_BUFFER_SIZE = 1024;

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final EventHandler handler;

    /**
     * Constructs a new Reactor listening on the specified port.
     *
     * @param port    The port to bind the server socket channel to. Pass {@code 0} to let the OS
     *                choose a free port, then read it back with {@link #getPort()}.
     * @param handler The handler invoked for data arriving on any accepted connection.
     * @throws IOException If an I/O error occurs while opening or configuring the channels.
     */
    public Reactor(int port, EventHandler handler) throws IOException {
        this.handler = handler;
        this.selector = Selector.open();
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Returns the port this reactor is actually listening on, which matters when it was constructed
     * with port {@code 0}.
     *
     * @return the bound local port.
     */
    public int getPort() {
        return serverSocketChannel.socket().getLocalPort();
    }

    /**
     * Main execution loop of the Reactor, dispatching readiness events until the thread is
     * interrupted or {@link #stop()} is called. This method should be invoked on its own thread.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    try {
                        if (key.isAcceptable()) {
                            acceptConnection();
                        } else if (key.isReadable()) {
                            readFrom(key);
                        }
                    } catch (IOException e) {
                        // One failed connection must not take the reactor down with it.
                        closeQuietly(key);
                    }
                }
            }
        } catch (ClosedSelectorException e) {
            // stop() closed the selector; this is the normal shutdown path.
        } catch (IOException e) {
            throw new UncheckedIOException("Reactor loop failed", e);
        }
    }

    /**
     * Accepts a pending connection and registers it for read readiness.
     * <p>
     * Registering the accepted channel is what keeps the connection alive in the reactor. Dropping
     * the returned channel here would leak the socket and mean the connection is never served.
     */
    private void acceptConnection() throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        if (client != null) {
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        }
    }

    /**
     * Drains a readable connection and dispatches the bytes to the handler, closing the connection
     * once the peer has signalled end of stream.
     */
    private void readFrom(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_SIZE);

        int read = channel.read(buffer);
        if (read < 0) {
            // End of stream: the peer closed its side.
            key.cancel();
            channel.close();
            return;
        }
        if (read == 0) {
            return;
        }

        buffer.flip();
        handler.onRead(channel, buffer);
    }

    /**
     * Closes the channel behind a key, suppressing any secondary failure so that cleanup of one
     * broken connection cannot abort the dispatch loop.
     */
    private static void closeQuietly(SelectionKey key) {
        key.cancel();
        try {
            key.channel().close();
        } catch (IOException ignored) {
            // Nothing useful left to do for a connection we are already discarding.
        }
    }

    /**
     * Stops the Reactor by closing the selector and the server socket channel, which unblocks the
     * dispatch loop and lets {@link #run()} return.
     *
     * @throws IOException If an I/O error occurs while closing the selector or the server channel.
     */
    public void stop() throws IOException {
        selector.close();
        serverSocketChannel.close();
    }
}
