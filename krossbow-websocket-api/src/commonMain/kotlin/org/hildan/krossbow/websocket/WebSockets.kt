package org.hildan.krossbow.websocket

interface KWebSocketClient {

    /**
     * Opens a web socket connection and suspends until the connection is OPEN.
     */
    suspend fun connect(url: String): KWebSocketSession
}

interface KWebSocketSession {

    var listener: KWebSocketListener

    suspend fun sendText(frameText: String)

    suspend fun sendBinary(frameData: ByteArray)

    suspend fun close()
}

interface KWebSocketListener {

    suspend fun onBinaryMessage(bytes: ByteArray)

    suspend fun onTextMessage(text: String)

    suspend fun onError(error: Throwable)

    suspend fun onClose()
}

object NoopWebSocketListener : KWebSocketListener {
    override suspend fun onBinaryMessage(bytes: ByteArray) = Unit
    override suspend fun onTextMessage(text: String) = Unit
    override suspend fun onError(error: Throwable) = Unit
    override suspend fun onClose() = Unit
}
