package org.hildan.krossbow.stomp.heartbeats

import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketSession

// If the sender has no real STOMP frame to send, it MUST send an end-of-line (EOL)
// https://stomp.github.io/stomp-specification-1.2.html#Heart-beating

internal suspend fun WebSocketSession.sendHeartBeat() {
    sendText("\n")
}

internal suspend fun WebSocketSession.closeForMissingHeartBeat() {
    close(WebSocketCloseCodes.PROTOCOL_ERROR, "Missing heart beat")
}

internal fun WebSocketFrame.isHeartBeat(): Boolean = when (this) {
    is WebSocketFrame.Text -> text == "\n" || text == "\r\n"
    is WebSocketFrame.Binary -> bytes.isEOL()
    else -> false
}

private const val CR = '\r'.toByte()
private const val LF = '\n'.toByte()

private fun ByteArray.isEOL() = when (size) {
    1 -> get(0) == LF
    2 -> get(0) == CR && get(1) == LF
    else -> false
}
