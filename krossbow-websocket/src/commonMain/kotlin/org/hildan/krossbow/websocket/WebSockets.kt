package org.hildan.krossbow.websocket

interface KWebSocket {

    /**
     * Opens a web socket connection and suspends until the connection is OPEN.
     */
    suspend fun connect(url: String): KWebSocketSession
}

interface KWebSocketSession {

    var listener: KWebSocketListener

    suspend fun send(frameData: ByteArray)

    suspend fun close()
}

interface KWebSocketListener {

    suspend fun onBinaryMessage(bytes: ByteArray)

    suspend fun onTextMessage(text: String)

    suspend fun onError(error: Throwable)

    suspend fun onClose()
}

object NoopWebsocketListener : KWebSocketListener {
    override suspend fun onBinaryMessage(bytes: ByteArray) = Unit
    override suspend fun onTextMessage(text: String) = Unit
    override suspend fun onError(error: Throwable) = Unit
    override suspend fun onClose() = Unit
}