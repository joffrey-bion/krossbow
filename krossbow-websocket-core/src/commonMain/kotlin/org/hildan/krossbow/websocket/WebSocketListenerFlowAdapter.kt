package org.hildan.krossbow.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Adapter between listener calls and a web socket "incoming" frames flow.
 * This is to make it easier to bridge listener-based APIs with the flow-based API of Krossbow.
 * Methods of this listener never fail, but can send failure through the frames channel to the consumers.
 */
class WebSocketListenerFlowAdapter(
    bufferSize: Int = Channel.BUFFERED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
) {
    private val frames: Channel<WebSocketFrame> = Channel(bufferSize, onBufferOverflow)

    /**
     * The flow of incoming web socket frames.
     * This flow completes when the web socket connection is closed or an error occurs.
     */
    val incomingFrames: Flow<WebSocketFrame> = frames.receiveAsFlow()

    private val partialTextMessageHandler = PartialTextMessageHandler {
        frames.send(WebSocketFrame.Text(it.toString()))
    }

    private val partialBinaryMessageHandler = PartialBinaryMessageHandler {
        frames.send(WebSocketFrame.Binary(it))
    }

    /**
     * Callback for binary messages (potentially partial frames).
     *
     * The given [bytes] array can be reused right after this method returns.
     *
     * If [isLast] is true, the web socket frame is considered complete and the full (aggregated) message is sent to
     * the [incomingFrames] flow as a [WebSocketFrame.Binary] frame.
     * Otherwise, the message is simply appended to a buffer and nothing happens in the [incomingFrames] flow.
     * More partial messages are expected in this case.
     */
    suspend fun onBinaryMessage(bytes: ByteArray, isLast: Boolean = true) {
        partialBinaryMessageHandler.processMessage(bytes, isLast)
    }

    /**
     * Callback for text messages (potentially partial frames).
     *
     * The given [text] CharSequence can be reused right after this method returns.
     *
     * If [isLast] is true, the web socket frame is considered complete and the full (aggregated) message is sent to
     * the [incomingFrames] flow as a [WebSocketFrame.Text] frame.
     * Otherwise, the message is simply appended to a buffer and nothing happens in the [incomingFrames] flow.
     * More partial messages are expected in this case.
     */
    suspend fun onTextMessage(text: CharSequence, isLast: Boolean = true) {
        partialTextMessageHandler.processMessage(text, isLast)
    }

    /**
     * Sends a [WebSocketFrame.Ping] frame to the [incomingFrames] flow.
     *
     * The given [bytes] array is used as-is in the frame and thus must not be modified later.
     */
    suspend fun onPing(bytes: ByteArray) {
        frames.send(WebSocketFrame.Ping(bytes))
    }

    /**
     * Sends a [WebSocketFrame.Pong] frame to the [incomingFrames] flow.
     *
     * The given [bytes] array is used as-is in the frame and thus must not be modified later.
     */
    suspend fun onPong(bytes: ByteArray) {
        frames.send(WebSocketFrame.Pong(bytes))
    }

    /**
     * Sends a [WebSocketFrame.Close] to the [incomingFrames] flow, and completes it normally.
     *
     * This adapter cannot be used anymore after a call to this method; calling any method may throw an exception.
     */
    @OptIn(ExperimentalCoroutinesApi::class) // for isClosedForSend
    suspend fun onClose(code: Int, reason: String?) {
        // At least with Spring's Jetty implementation, onClose may be called after onError
        // (for instance, if frame parsing fails with unknown opcode).
        // This means that this send(Close) can fail because the channel is already failed.
        if (frames.isClosedForSend) {
            return
        }
        frames.send(WebSocketFrame.Close(code, reason))
        frames.close()
        partialBinaryMessageHandler.close()
    }

    /**
     * Fails the [incomingFrames] flow with a [WebSocketException] with the given [message].
     *
     * Only [onClose] is safe to call after a call to this method, calling any other method may throw an exception.
     */
    fun onError(message: String) {
        frames.close(WebSocketException(message))
        partialBinaryMessageHandler.close()
    }

    /**
     * Fails the [incomingFrames] flow with a [WebSocketException] with the given [error] as cause.
     *
     * Only [onClose] is safe to call after a call to this method, calling any other method may throw an exception.
     */
    fun onError(error: Throwable?) {
        frames.close(WebSocketException(error?.message ?: "web socket error", cause = error))
        partialBinaryMessageHandler.close()
    }
}

/**
 * An adapter similar to [WebSocketListenerFlowAdapter], but with an unlimited buffer and non-suspending callback
 * functions. This is useful for bridging implementations that do not support backpressure (like the browser
 * WebSocket API).
 *
 * This implementation does not support partial messages.
 */
class UnboundedWsListenerFlowAdapter {

    private val frames: Channel<WebSocketFrame> = Channel(capacity = Channel.UNLIMITED)

    /**
     * The channel of incoming web socket frames.
     * This channel is closed when the web socket connection is closed
     */
    val incomingFrames: Flow<WebSocketFrame> = frames.receiveAsFlow()

    /**
     * Sends a [WebSocketFrame.Binary] frame to the [incomingFrames] flow.
     *
     * The given [bytes] array is used as-is in the frame and thus must not be modified later.
     */
    fun onBinaryMessage(bytes: ByteArray) = frames.trySend(WebSocketFrame.Binary(bytes))

    /**
     * Sends a [WebSocketFrame.Text] frame to the [incomingFrames] flow.
     */
    fun onTextMessage(text: String) = frames.trySend(WebSocketFrame.Text(text))

    /**
     * Sends a [WebSocketFrame.Ping] frame to the [incomingFrames] flow.
     *
     * The given [bytes] array is used as-is in the frame and thus must not be modified later.
     */
    fun onPing(bytes: ByteArray) = frames.trySend(WebSocketFrame.Ping(bytes))

    /**
     * Sends a [WebSocketFrame.Pong] frame to the [incomingFrames] flow.
     *
     * The given [bytes] array is used as-is in the frame and thus must not be modified later.
     */
    fun onPong(bytes: ByteArray)= frames.trySend(WebSocketFrame.Pong(bytes))

    /**
     * Sends a [WebSocketFrame.Close] to the [incomingFrames] flow, and completes it normally.
     *
     * This adapter cannot be used anymore after a call to this method; calling any method may throw an exception.
     */
    fun onClose(code: Int, reason: String?): ChannelResult<Unit> {
        val result = frames.trySend(WebSocketFrame.Close(code, reason))
        frames.close()
        return result
    }

    /**
     * Fails the [incomingFrames] flow with a [WebSocketException] with the given [message].
     *
     * Only [onClose] is safe to call after a call to this method, calling any other method may throw an exception.
     */
    fun onError(message: String) {
        frames.close(WebSocketException(message))
    }

    /**
     * Fails the [incomingFrames] flow with a [WebSocketException] with the given [error] as cause.
     *
     * Only [onClose] is safe to call after a call to this method, calling any other method may throw an exception.
     */
    fun onError(error: Throwable?) {
        frames.close(WebSocketException(error?.message ?: "web socket error", cause = error))
    }
}
