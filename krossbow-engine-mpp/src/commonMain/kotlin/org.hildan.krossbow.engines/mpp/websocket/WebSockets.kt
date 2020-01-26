package org.hildan.krossbow.engines.mpp.websocket

import kotlinx.coroutines.channels.ReceiveChannel

interface WebSocket {

    suspend fun connect(url: String): WebSocketSession
}

interface WebSocketSession {

    val incomingFrames: ReceiveChannel<ByteArray>

    suspend fun send(frameData: ByteArray)

    suspend fun close()
}
