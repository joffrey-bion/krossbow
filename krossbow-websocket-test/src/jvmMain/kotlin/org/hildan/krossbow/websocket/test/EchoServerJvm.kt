package org.hildan.krossbow.websocket.test

import com.pusher.java_websocket.WebSocket
import com.pusher.java_websocket.handshake.ClientHandshake
import com.pusher.java_websocket.server.WebSocketServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.nio.ByteBuffer

internal actual suspend fun runAlongEchoWSServer(block: suspend (server: TestServer) -> Unit) {
    val server = EchoWebSocketServer()
    server.startAndAwaitPort()
    block(server.asTestServer())
    server.stop()
}

internal class EchoWebSocketServer(port: Int = 0) : WebSocketServer(InetSocketAddress(port)) {

    @Volatile
    var lastConnectedSocket: WebSocket? = null

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        lastConnectedSocket = conn
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

private fun EchoWebSocketServer.asTestServer() = object : TestServer {
    override val port: Int
        get() = this@asTestServer.port

    private val socket
        get() = lastConnectedSocket ?: error("No connected client")

    override fun send(text: String) {
        socket.send(text)
    }

    override fun send(bytes: ByteArray) {
        socket.send(bytes)
    }

    override fun close(code: Int, message: String?) {
        socket.close(code, message)
    }

    override fun closeConnection(code: Int, message: String?) {
        socket.closeConnection(code, message)
    }
}
