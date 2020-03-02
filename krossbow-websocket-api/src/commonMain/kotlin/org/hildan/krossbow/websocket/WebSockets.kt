package org.hildan.krossbow.websocket

interface WebSocketClient {

    /**
     * Opens a web socket connection and suspends until the connection is OPEN.
     */
    suspend fun connect(url: String): WebSocketSession
}

interface WebSocketSession {

    var listener: WebSocketListener

    suspend fun sendText(frameText: String)

    suspend fun sendBinary(frameData: ByteArray)

    suspend fun close()
}

interface WebSocketListener {

    suspend fun onBinaryMessage(bytes: ByteArray)

    suspend fun onTextMessage(text: String)

    suspend fun onError(error: Throwable)

    suspend fun onClose()
}

object NoopWebSocketListener : WebSocketListener {
    override suspend fun onBinaryMessage(bytes: ByteArray) = Unit
    override suspend fun onTextMessage(text: String) = Unit
    override suspend fun onError(error: Throwable) = Unit
    override suspend fun onClose() = Unit
}
