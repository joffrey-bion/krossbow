package org.hildan.krossbow.websocket

import kotlinx.io.bytestring.*

/**
 * A web socket frame.
 */
sealed class WebSocketFrame {

    /**
     * A web socket text frame (0x1).
     */
    data class Text(val text: String) : WebSocketFrame()

    /**
     * A web socket binary frame (0x2).
     */
    data class Binary(val bytes: ByteString) : WebSocketFrame()

    /**
     * A web socket ping frame (0x9).
     */
    data class Ping(val bytes: ByteString) : WebSocketFrame()

    /**
     * A web socket pong frame (0xA).
     */
    data class Pong(val bytes: ByteString) : WebSocketFrame()

    /**
     * A web socket close frame (0x8).
     */
    data class Close(val code: Int, val reason: String?) : WebSocketFrame()
}
