package org.alxkm.patterns.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Handles the application-level work for one readiness event dispatched by a {@link Reactor}.
 * <p>
 * Splitting this out is the point of the Reactor pattern: the reactor owns demultiplexing (which
 * channel is ready?) and the handler owns interpretation (what do those bytes mean?). Keep
 * implementations short and non-blocking -- they run on the reactor thread, so a slow handler stalls
 * every other connection.
 */
@FunctionalInterface
public interface EventHandler {

    /**
     * Called when bytes have been read from a connection.
     *
     * @param channel the connection the data arrived on, still registered with the reactor.
     * @param data    a buffer positioned at 0 and limited to the bytes just read.
     * @throws IOException if handling the data fails; the reactor closes the connection in response.
     */
    void onRead(SocketChannel channel, ByteBuffer data) throws IOException;
}
