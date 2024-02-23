package org.hildan.krossbow.test.server

import kotlinx.coroutines.*
import org.java_websocket.*
import org.java_websocket.handshake.*
import org.java_websocket.server.*
import java.net.*
import java.nio.*

private val StandardHandshakeRequestHeaders = setOf(
    "Accept",
    "Accept-Charset",
    "Accept-Encoding",
    "Accept-Language", // sent by Apple client
    "Cache-Control", // sent by browser WS
    "Connection",
    "Content-Length",
    "Host",
    "Origin",
    "Pragma", // sent by browser WS
    "Sec-WebSocket-Key",
    "Sec-WebSocket-Protocol",
    "Sec-WebSocket-Extensions",
    "Sec-WebSocket-Version",
    "Upgrade",
    "User-Agent",
)

internal class EchoWebSocketServer(port: Int = 0) : WebSocketServer(InetSocketAddress(port)) {

    @Volatile
    var openedSocket: CompletableDeferred<WebSocket> = CompletableDeferred()

    override fun onStart() {
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        openedSocket.complete(conn ?: error("onOpen got a null WebSocket"))
        val headerNames = handshake?.iterateHttpFields()?.asSequence()?.toSet() ?: emptySet()
        val customHeaders = (headerNames - StandardHandshakeRequestHeaders).sorted()
        if (customHeaders.isNotEmpty()) {
            conn.send("custom-headers:${customHeaders.joinToString { "$it=${handshake?.getFieldValue(it)}" }}")
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        conn?.send(message)
    }

    override fun onMessage(conn: WebSocket?, message: ByteBuffer) {
        val bytes = ByteArray(message.remaining())
        message.get(bytes)
        conn?.send(bytes)
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
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
