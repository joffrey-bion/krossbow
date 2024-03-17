package org.hildan.krossbow.websocket

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.io.*
import kotlinx.io.bytestring.*

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
        frames.send(WebSocketFrame.Text(it))
    }

    private val partialBinaryMessageHandler = PartialBinaryMessageHandler {
        frames.send(WebSocketFrame.Binary(it))
    }

    /**
     * Callback for binary messages (potentially partial frames).
     *
     * Use this overload when the data of the frame can be safely wrapped without copy into a [ByteString] for
     * processing. If the data is provided as a reusable buffer that must be consumed/copied, use the other overload to
     * write directly to the [Sink] buffer to avoid copying data around.
     *
     * If [isLast] is true, the web socket frame is considered complete and the full (aggregated) message is sent to
     * the [incomingFrames] flow as a [WebSocketFrame.Binary] frame.
     * Otherwise, the message is simply appended to a buffer and nothing happens in the [incomingFrames] flow.
     * More partial messages are expected in this case.
     */
    suspend fun onBinaryMessage(bytes: ByteString, isLast: Boolean = true) {
        partialBinaryMessageHandler.processMessage(bytes, isLast)
    }

    /**
     * Callback for binary messages (potentially partial frames).
     *
     * Use this overload when the data of the frame is provided as a reusable buffer that must be consumed/copied, and
     * use [writeData] to transfer bytes from the received buffer into this adapter's buffer.
     * If the data of the frame can be safely wrapped without copy into a [ByteString] for processing, use the other
     * overload taking a [ByteString] as parameter.
     *
     * If [isLast] is true, the web socket frame is considered complete and the full (aggregated) message is sent to
     * the [incomingFrames] flow as a [WebSocketFrame.Binary] frame.
     * Otherwise, the message is simply appended to a buffer and nothing happens in the [incomingFrames] flow.
     * More partial messages are expected in this case.
     */
    suspend fun onBinaryMessage(isLast: Boolean = true, writeData: Sink.() -> Unit) {
        partialBinaryMessageHandler.processMessage(isLast, writeData)
    }

    /**
     * Callback for text messages (potentially partial frames).
     *
     * Use this overload when the data of the frame can be converted without copy into a [String] for processing.
     * If the data is provided as a reusable (char) buffer that must be consumed/copied, use the other overload to
     * write directly to the [Sink] buffer to avoid copying data around.
     *
     * If [isLast] is true, the web socket frame is considered complete and the full (aggregated) message is sent to
     * the [incomingFrames] flow as a [WebSocketFrame.Text] frame.
     * Otherwise, the message is simply appended to a buffer and nothing happens in the [incomingFrames] flow.
     * More partial messages are expected in this case.
     */
    suspend fun onTextMessage(text: String, isLast: Boolean = true) {
        partialTextMessageHandler.processMessage(text, isLast)
    }

    /**
     * Callback for text messages (potentially partial frames).
     *
     * Use this overload when the data of the frame is provided as a reusable buffer that must be consumed/copied, and
     * use [writeData] to transfer data from the received buffer into this adapter's buffer.
     * If the data of the frame can be converted without copy into a [String] for processing, use the other overload
     * taking a [String] as parameter.
     *
     * If [isLast] is true, the web socket frame is considered complete and the full (aggregated) message is sent to
     * the [incomingFrames] flow as a [WebSocketFrame.Text] frame.
     * Otherwise, the message is simply appended to a buffer and nothing happens in the [incomingFrames] flow.
     * More partial messages are expected in this case.
     */
    suspend fun onTextMessage(isLast: Boolean = true, writeData: Sink.() -> Unit) {
        partialTextMessageHandler.processMessage(isLast, writeData)
    }

    /**
     * Sends a [WebSocketFrame.Ping] frame with the given [bytes] to the [incomingFrames] flow.
     */
    suspend fun onPing(bytes: ByteString) {
        frames.send(WebSocketFrame.Ping(bytes))
    }

    /**
     * Sends a [WebSocketFrame.Pong] frame with the given [bytes] to the [incomingFrames] flow.
     */
    suspend fun onPong(bytes: ByteString) {
        frames.send(WebSocketFrame.Pong(bytes))
    }

    /**
     * Sends a [WebSocketFrame.Close] to the [incomingFrames] flow, and completes it normally.
     *
     * This adapter cannot be used anymore after a call to this method; calling any method may throw an exception.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun onClose(code: Int, reason: String?) {
        // At least with Spring's Jetty implementation, onClose may be called after onError
        // (for instance, if frame parsing fails with unknown opcode).
        // In such cases, we don't need to send a Close frame (and we can't) as the channel is already closed or failed.
        if (frames.isClosedForSend) {
            return
        }
        try {
            frames.send(WebSocketFrame.Close(code, reason))
            frames.close()
        } catch (e: ClosedSendChannelException) {
            // If the channel was concurrently closed (despite the isClosedForSend check, there can be a race),
            // we don't need to send the Close frame because it has already been done or the channel is failed.
            // Therefore, it's ok to ignore this exception.
            return
        } finally {
            partialBinaryMessageHandler.close()
        }
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
     * The flow of incoming web socket frames.
     * The underlying channel is closed when the web socket connection is closed
     */
    val incomingFrames: Flow<WebSocketFrame> = frames.receiveAsFlow()

    /**
     * Sends a [WebSocketFrame.Binary] frame to the [incomingFrames] flow.
     */
    fun onBinaryMessage(bytes: ByteString) = frames.trySend(WebSocketFrame.Binary(bytes))

    /**
     * Sends a [WebSocketFrame.Text] frame to the [incomingFrames] flow.
     */
    fun onTextMessage(text: String) = frames.trySend(WebSocketFrame.Text(text))

    /**
     * Sends a [WebSocketFrame.Ping] frame to the [incomingFrames] flow.
     */
    fun onPing(bytes: ByteString) = frames.trySend(WebSocketFrame.Ping(bytes))

    /**
     * Sends a [WebSocketFrame.Pong] frame to the [incomingFrames] flow.
     */
    fun onPong(bytes: ByteString) = frames.trySend(WebSocketFrame.Pong(bytes))

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
