package org.hildan.krossbow.websocket.test

import com.pusher.java_websocket.WebSocket
import com.pusher.java_websocket.handshake.ClientHandshake
import com.pusher.java_websocket.server.WebSocketServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.nio.ByteBuffer

internal actual suspend fun runAlongEchoWSServer(
    onOpenActions: ActionsBuilder.() -> Unit,
    block: suspend (port: Int) -> Unit,
) {
    val server = EchoWebSocketServer(ActionsBuilder().apply(onOpenActions).build())
    val port = server.startAndAwaitPort()
    block(port)
    server.stop()
}

internal class EchoWebSocketServer(
    private val onOpenActions: List<ServerAction>,
    port: Int = 0,
) : WebSocketServer(InetSocketAddress(port)) {

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        for (a in onOpenActions) {
            when (a) {
                is ServerAction.SendTextFrame -> conn?.send(a.message)
                is ServerAction.SendBinaryFrame -> conn?.send(a.data)
                is ServerAction.Close -> conn?.close(a.code, a.reason)
            }
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
