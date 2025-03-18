package org.hildan.krossbow.test.server

import kotlinx.coroutines.*
import org.java_websocket.*
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.*
import org.java_websocket.protocols.*
import org.java_websocket.server.*
import java.net.*
import java.nio.*

private val Draft6455Default = Draft_6455()
private val Draft6455WithStomp12 = Draft_6455(emptyList(), listOf(Protocol("v12.stomp")))

internal class EchoWebSocketServer(port: Int = 0) : WebSocketServer(
    InetSocketAddress(port),
    listOf(Draft6455WithStomp12, Draft6455Default),
) {
    override fun onStart() {
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val uri = URI.create(handshake.resourceDescriptor)
        if (uri.path == "/sendHandshakeHeaders") {
            conn.sendMessageWithHeaders(handshake)
        }
    }

    private fun WebSocket.sendMessageWithHeaders(handshake: ClientHandshake) {
        val headerNames = handshake.iterateHttpFields().asSequence().toList()
        val headersData = headerNames.joinToString("\n") { "$it=${handshake.getFieldValue(it)}" }
        send(headersData)
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
