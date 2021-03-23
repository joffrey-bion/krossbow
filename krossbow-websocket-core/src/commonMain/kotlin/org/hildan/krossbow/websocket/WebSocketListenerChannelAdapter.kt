package org.hildan.krossbow.websocket

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
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

    suspend fun onClose(code: Int, reason: String?) {
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
