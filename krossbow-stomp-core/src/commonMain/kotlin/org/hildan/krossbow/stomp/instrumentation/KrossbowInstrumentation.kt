package org.hildan.krossbow.stomp.instrumentation

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.websocket.WebSocketFrame

/**
 * An interface to hook some pieces of code in different places of internal Krossbow execution.
 * This is primarily meant for monitoring, logging or debugging.
 */
interface KrossbowInstrumentation {

    /**
     * Called upon reception of every websocket frame.
     */
    suspend fun onWebSocketFrameReceived(frame: WebSocketFrame) = Unit

    /**
     * Called right after successfully decoding a websocket frame into a STOMP frame.
     * This callback is called even for STOMP ERROR frames (before failure).
     */
    suspend fun onFrameDecoded(originalFrame: WebSocketFrame, decodedFrame: StompFrame) = Unit

    /**
     * Called right after successfully sending a STOMP [frame] to the server.
     */
    suspend fun onStompFrameSent(frame: StompFrame) = Unit

    /**
     * Called after the websocket is closed.
     *
     * In case of a normal closure, the [cause] argument is null.
     * The [cause] is non-null if the closure of the web socket is due to an exception in the STOMP client (STOMP ERROR
     * frame, unexpected websocket close frame, missing heart-beat, decoding errors, etc.).
     *
     * Note that this callback is not called if the websocket client itself failed with an exception.
     * In that case, [onWebSocketClientError] is called instead.
     */
    suspend fun onWebSocketClosed(cause: Throwable?) = Unit

    /**
     * Called if some exception is thrown by the websocket client.
     */
    suspend fun onWebSocketClientError(exception: Throwable) = Unit

    /**
     * Returns a new instrumentation chain that runs this instrumentation and the given [instrumentation] in each
     * callback.
     */
    operator fun plus(instrumentation: KrossbowInstrumentation): KrossbowInstrumentation =
        KrossbowInstrumentationChain(listOf(this, instrumentation))
}

/**
 * A [KrossbowInstrumentation] that runs multiple instrumentations in sequence for each callback.
 */
private class KrossbowInstrumentationChain(
    private val instrumentations: List<KrossbowInstrumentation>
) : KrossbowInstrumentation {

    override suspend fun onWebSocketFrameReceived(frame: WebSocketFrame) {
        instrumentations.forEach { it.onWebSocketFrameReceived(frame) }
    }

    override suspend fun onFrameDecoded(originalFrame: WebSocketFrame, decodedFrame: StompFrame) {
        instrumentations.forEach { it.onFrameDecoded(originalFrame, decodedFrame) }
    }

    override suspend fun onStompFrameSent(frame: StompFrame) {
        instrumentations.forEach { it.onStompFrameSent(frame) }
    }

    override suspend fun onWebSocketClosed(cause: Throwable?) {
        instrumentations.forEach { it.onWebSocketClosed(cause) }
    }

    override suspend fun onWebSocketClientError(exception: Throwable) {
        instrumentations.forEach { it.onWebSocketClientError(exception) }
    }

    // this is just an optimization to avoid nesting KrossbowInstrumentationChain objects
    override operator fun plus(instrumentation: KrossbowInstrumentation): KrossbowInstrumentation =
        KrossbowInstrumentationChain(instrumentations + instrumentation)
}
