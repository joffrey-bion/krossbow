package org.hildan.krossbow.test.server

import kotlinx.coroutines.*
import org.java_websocket.*
import org.java_websocket.handshake.*
import org.java_websocket.server.*
import java.net.*
import java.nio.*
import kotlin.time.*

internal class EchoWebSocketServer(port: Int = 0) : WebSocketServer(InetSocketAddress(port)) {

    override fun onStart() {
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val uri = URI.create(handshake.resourceDescriptor)
        println("Connection to URI $uri")

        if (uri.path == "/sendHandshakeHeaders") {
            conn.sendMessageWithHeaders(handshake)
        } else {
            println("Not sending headers frame for URI $uri")
        }
    }

    private fun WebSocket.sendMessageWithHeaders(handshake: ClientHandshake) {
        val headerNames = handshake.iterateHttpFields().asSequence().toList()
        val headersData = headerNames.joinToString("\n") { "$it=${handshake.getFieldValue(it)}" }
        println("Sending message with headers...")
        send(headersData)
        println("Headers frame sent!")
    }

    override fun onMessage(conn: WebSocket, message: String?) {
        conn.send(message)
    }

    override fun onMessage(conn: WebSocket, message: ByteBuffer) {
        val bytes = ByteArray(message.remaining())
        message.get(bytes)
        conn.send(bytes)
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
    }

    suspend fun startAndAwaitPort(timeoutMillis: Long = 1000): Int {
        start()
        return awaitPort(timeoutMillis)
    }

    private suspend fun awaitPort(timeoutMillis: Long): Int = withTimeout(timeoutMillis) {
        while (port <= 0) {
            delay(10)
        }
        port
    }
}

private fun URI.queryAsMap() = query.split("&")
    .map { it.split("=") }
    .associate { it[0] to it[1] }

