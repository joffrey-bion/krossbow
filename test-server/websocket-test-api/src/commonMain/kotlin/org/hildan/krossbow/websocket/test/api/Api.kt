package org.hildan.krossbow.websocket.test.api

//interface TestServer {
//
//    val host: String
//    val wsPort: Int
//    val httpPort: Int
//
//    fun socket(id: String): ServerSocket
//
//    fun stop()
//}
//
//interface ServerSocket {
//
//    /**
//     * Sends text data to the last connected client.
//     */
//    suspend fun send(text: String)
//
//    /**
//     * Sends binary data to the last connected client.
//     */
//    suspend fun send(bytes: ByteArray)
//
//    /**
//     * Sends the closing handshake. May be sent in response to another handshake.
//     */
//    suspend fun close(code: Int, reason: String? = null)
//
//    /**
//     * Closes the connection immediately without a proper close handshake.
//     *
//     * The code and the message therefore won't be transferred over the wire also they will be forwarded to
//     * onClose/onWebsocketClose.
//     */
//    suspend fun closeConnection(code: Int, message: String? = null)
//}
