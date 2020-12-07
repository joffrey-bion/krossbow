package org.hildan.krossbow.websocket.test

import com.pusher.java_websocket.WebSocket
import com.pusher.java_websocket.handshake.ClientHandshake
import com.pusher.java_websocket.server.WebSocketServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.nio.ByteBuffer

actual fun getDefaultAutobahnTestServerHost(): String? = System.getenv("AUTOBAHN_SERVER_HOST")

actual fun getDefaultAutobahnTestServerPort(): Int? = System.getenv("AUTOBAHN_SERVER_TCP_9001")?.toInt()

actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit) = runBlocking { block() }

internal actual suspend fun runAlongEchoWSServer(block: suspend (port: Int) -> Unit) {
    val server = EchoWebSocketServer()
    val port = server.startAndAwaitPort()
    block(port)
    server.stop()
}

class EchoWebSocketServer(port: Int = 0) : WebSocketServer(InetSocketAddress(port)) {

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
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
