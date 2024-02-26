package org.hildan.krossbow.test.server

//import kotlinx.coroutines.*
//import kotlinx.coroutines.channels.*
//import org.java_websocket.*
//import org.java_websocket.handshake.*
//import org.java_websocket.server.*
//import java.net.*
//import java.nio.*
//
//suspend fun startWebSocketServer(address: InetSocketAddress = InetSocketAddress(0)): KWebSocketServer {
//    val server = EventWebSocketServer(address).apply {
//        start()
//        awaitStarted()
//    }
//    return KWebSocketServerAdapter(server)
//}
//
//interface KWebSocketServer {
//    val port: Int
//    val events: ReceiveChannel<WSServerEvent>
//    fun stop()
//}
//
//// can't implement directly because of "accidental override"
//private class KWebSocketServerAdapter(private val server: EventWebSocketServer): KWebSocketServer {
//    override val port: Int
//        get() = server.port
//    override val events: ReceiveChannel<WSServerEvent>
//        get() = server.events
//
//    override fun stop() = server.stop()
//}
//
//sealed interface WSServerEvent {
//
//    val socket: WebSocket?
//
//    data class Handshake(
//        override val socket: WebSocket,
//        val url: String,
//        val headers: Map<String, String>,
//        val body: ByteArray,
//    ) : WSServerEvent
//
//    data class FrameReceived(
//        override val socket: WebSocket,
//        val frame: Frame,
//    ) : WSServerEvent
//
//    data class Error(
//        override val socket: WebSocket?,
//        val exception: Exception?,
//    ) : WSServerEvent
//}
//
//sealed interface Frame {
//    data class TextMessage(val content: String) : Frame
//    data class BinaryMessage(val bytes: ByteArray) : Frame
//    data class Close(val code: Int, val reason: String?, val fromClient: Boolean) : Frame
//}
//
//private class EventWebSocketServer(address: InetSocketAddress) : WebSocketServer(address) {
//
//    val events: Channel<WSServerEvent> = Channel(Channel.BUFFERED)
//
//    private val started: CompletableDeferred<Unit> = CompletableDeferred()
//
//    override fun onStart() {
//        started.complete(Unit)
//    }
//
//    suspend fun awaitStarted() {
//        started.await()
//    }
//
//    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
//        val handshakeEvent = WSServerEvent.Handshake(
//            socket = conn,
//            url = handshake.resourceDescriptor,
//            headers = handshake.readHeaders(),
//            body = handshake.content,
//        )
//        events.trySendBlocking(handshakeEvent)
//    }
//
//    override fun onMessage(conn: WebSocket, message: String) {
//        conn.send(message)
//        events.trySendBlocking(WSServerEvent.FrameReceived(conn, Frame.TextMessage(message)))
//    }
//
//    override fun onMessage(conn: WebSocket, message: ByteBuffer) {
//        val bytes = ByteArray(message.remaining())
//        message.get(bytes)
//        conn.send(bytes)
//        events.trySendBlocking(WSServerEvent.FrameReceived(conn, Frame.BinaryMessage(bytes)))
//    }
//
//    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
//        events.trySendBlocking(WSServerEvent.FrameReceived(conn, Frame.Close(code, reason, remote)))
//    }
//
//    override fun onError(conn: WebSocket?, ex: Exception?) {
//        events.trySendBlocking(WSServerEvent.Error(conn, ex))
//    }
//}
//
//private fun ClientHandshake.readHeaders(): Map<String, String> {
//    val headerNames = iterateHttpFields()?.asSequence()?.toSet() ?: emptySet()
//    return headerNames.associateWith { getFieldValue(it) }
//}
