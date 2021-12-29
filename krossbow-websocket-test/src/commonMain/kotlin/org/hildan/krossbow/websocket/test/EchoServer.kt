package org.hildan.krossbow.websocket.test

import org.hildan.krossbow.websocket.WebSocketCloseCodes

internal expect suspend fun runAlongEchoWSServer(block: suspend (server: TestServer) -> Unit)

internal interface TestServer {

    val port: Int

    /**
     * Sends text data to the last connected client.
     */
    fun send(text: String)

    /**
     * Sends binary data to the last connected client.
     */
    fun send(bytes: ByteArray)

    /**
     * Sends the closing handshake. May be sent in response to another handshake.
     */
    fun close(code: Int = WebSocketCloseCodes.NORMAL_CLOSURE, message: String? = null)

    /**
     * Closes the connection immediately without a proper close handshake.
     *
     * The code and the message therefore won't be transferred over the wire also they will be forwarded to
     * onClose/onWebsocketClose.
     */
    fun closeConnection(code: Int = WebSocketCloseCodes.NO_STATUS_CODE, message: String? = null)
}
