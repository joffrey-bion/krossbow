package org.hildan.krossbow.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Adapter between listener calls and a web socket "incoming" frames channel.
 * This is to make it easier to bridge listener-based APIs with the channel-based API of Krossbow.
 * Methods of this listener never fail, but can send failure through the frames channel to the consumers.
 */
class WebSocketListenerChannelAdapter(
    bufferSize: Int = Channel.BUFFERED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
) {
    /**
     * The channel of incoming web socket frames.
     * This channel is closed when the web socket connection is closed
     */
    val incomingFrames: ReceiveChannel<WebSocketFrame>
        get() = frames

    private val frames: Channel<WebSocketFrame> = Channel(bufferSize, onBufferOverflow)

    private val partialTextMessageHandler = PartialTextMessageHandler {
        frames.send(WebSocketFrame.Text(it.toString()))
    }

    private val partialBinaryMessageHandler = PartialBinaryMessageHandler {
        frames.send(WebSocketFrame.Binary(it))
    }

    suspend fun onBinaryMessage(bytes: ByteArray, isLast: Boolean = true) {
        partialBinaryMessageHandler.processMessage(bytes, isLast)
    }

    suspend fun onTextMessage(text: CharSequence, isLast: Boolean = true) {
        partialTextMessageHandler.processMessage(text, isLast)
    }

    suspend fun onPing(bytes: ByteArray) {
        frames.send(WebSocketFrame.Ping(bytes))
    }

    suspend fun onPong(bytes: ByteArray) {
        frames.send(WebSocketFrame.Pong(bytes))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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

    fun onError(message: String) {
        frames.close(WebSocketException(message))
        partialBinaryMessageHandler.close()
    }

    fun onError(error: Throwable?) {
        frames.close(WebSocketException(error?.message ?: "web socket error", cause = error))
        partialBinaryMessageHandler.close()
    }
}

/**
 * An adapter similar to [WebSocketListenerChannelAdapter], but with an unlimited buffer and non-suspending callback
 * functions. This is useful for bridging implementations that do not support backpressure (like the browser
 * WebSocket API).
 *
 * This implementation does not support partial messages.
 */
class UnboundedWsListenerChannelAdapter {
    /**
     * The channel of incoming web socket frames.
     * This channel is closed when the web socket connection is closed
     */
    val incomingFrames: ReceiveChannel<WebSocketFrame>
        get() = frames

    private val frames: Channel<WebSocketFrame> = Channel(capacity = Channel.UNLIMITED)

    fun onBinaryMessage(bytes: ByteArray) = frames.trySend(WebSocketFrame.Binary(bytes))

    fun onTextMessage(text: String) = frames.trySend(WebSocketFrame.Text(text))

    fun onPing(bytes: ByteArray) = frames.trySend(WebSocketFrame.Ping(bytes))

    fun onPong(bytes: ByteArray)= frames.trySend(WebSocketFrame.Pong(bytes))

    fun onClose(code: Int, reason: String?): ChannelResult<Unit> {
        val result = frames.trySend(WebSocketFrame.Close(code, reason))
        frames.close()
        return result
    }

    fun onError(message: String) {
        frames.close(WebSocketException(message))
    }

    fun onError(error: Throwable?) {
        frames.close(WebSocketException(error?.message ?: "web socket error", cause = error))
    }
}
