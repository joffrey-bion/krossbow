package org.hildan.krossbow.websocket

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Adapter between listener calls and a web socket frames channel.
 * This is to make it easier to bridge listener-based APIs with the channel-based API of Krossbow.
 * Methods of this listener never fail, but can send failure through the frames channel to the consumers.
 */
class WebSocketListenerChannelAdapter {

    val incomingFrames: ReceiveChannel<WebSocketFrame>
        get() = frames

    private val frames: Channel<WebSocketFrame> = Channel()

    suspend fun onBinaryMessage(bytes: ByteArray) {
        runCatching {
            frames.send(WebSocketFrame.Binary(bytes))
        }
    }

    suspend fun onTextMessage(text: String) {
        runCatching {
            frames.send(WebSocketFrame.Text(text))
        }
    }

    suspend fun onEmptyMessage() {
        runCatching {
            frames.send(WebSocketFrame.Text(""))
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
