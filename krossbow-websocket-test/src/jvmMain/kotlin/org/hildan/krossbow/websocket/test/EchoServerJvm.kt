package org.hildan.krossbow.websocket.test

import com.pusher.java_websocket.WebSocket
import com.pusher.java_websocket.handshake.ClientHandshake
import com.pusher.java_websocket.server.WebSocketServer
import kotlinx.coroutines.CompletableDeferred
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
    var openedSocket: CompletableDeferred<WebSocket> = CompletableDeferred()

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        openedSocket.complete(conn ?: error("onOpen got a null WebSocket"))
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

    private suspend fun getSocket(): WebSocket = openedSocket.await()

    override suspend fun send(text: String) {
        getSocket().send(text)
    }

    override suspend fun send(bytes: ByteArray) {
        getSocket().send(bytes)
    }

    override suspend fun close(code: Int, message: String?) {
        getSocket().close(code, message)
    }

    override suspend fun closeConnection(code: Int, message: String?) {
        getSocket().closeConnection(code, message)
    }
}
