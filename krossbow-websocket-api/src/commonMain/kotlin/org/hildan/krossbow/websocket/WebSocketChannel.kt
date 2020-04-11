package org.hildan.krossbow.websocket

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully

/**
 * Adapter between listener calls and a web socket frames channel.
 * This is to make it easier to bridge listener-based APIs with the channel-based API of Krossbow.
 * Methods of this listener never fail, but can send failure through the frames channel to the consumers.
 */
class WebSocketListenerChannelAdapter {

    val incomingFrames: ReceiveChannel<WebSocketFrame>
        get() = frames

    private val frames: Channel<WebSocketFrame> = Channel()

    private val textBuilder = StringBuilder()

    private val bytesBuilder = BytePacketBuilder()

    suspend fun onBinaryMessage(bytes: ByteArray, isLast: Boolean = true) {
        runCatching {
            if (bytesBuilder.isEmpty && isLast) {
                // optimization: do not buffer complete messages
                frames.send(WebSocketFrame.Binary(bytes))
            } else {
                processPartialFrame(bytes, isLast)
            }
        }
    }

    suspend fun onTextMessage(text: CharSequence, isLast: Boolean = true) {
        runCatching {
            if (textBuilder.isEmpty() && isLast) {
                // optimization: do not buffer complete messages
                frames.send(WebSocketFrame.Text(text.toString()))
            } else {
                processPartialFrame(text, isLast)
            }
        }
    }

    private suspend fun processPartialFrame(bytes: ByteArray, isLast: Boolean) {
        bytesBuilder.writeFully(bytes)
        if (isLast) {
            val wholeFrameBytes = bytesBuilder.build().readBytes()
            frames.send(WebSocketFrame.Binary(wholeFrameBytes))
        }
    }

    private suspend fun processPartialFrame(text: CharSequence, isLast: Boolean) {
        textBuilder.append(text)
        if (isLast) {
            val wholeFrameText = textBuilder.toString()
            textBuilder.clear()
            frames.send(WebSocketFrame.Text(wholeFrameText))
        }
    }

    suspend fun onClose(code: Int, reason: String?) {
        runCatching {
            frames.send(WebSocketFrame.Close(code, reason))
            frames.close()
        }
    }

    fun onError(message: String) {
        frames.close(WebSocketException(message))
    }

    fun onError(error: Throwable?) {
        frames.close(WebSocketException(error?.message ?: "web socket error", cause = error))
    }
}

sealed class WebSocketFrame {

    data class Text(val text: String) : WebSocketFrame()

    class Binary(val bytes: ByteArray) : WebSocketFrame()

    data class Close(val code: Int, val reason: String?) : WebSocketFrame()
}
