package org.hildan.krossbow.websocket.spring

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.*
import kotlinx.coroutines.sync.*
import kotlinx.io.*
import kotlinx.io.bytestring.*
import org.hildan.krossbow.io.*
import org.hildan.krossbow.websocket.*
import org.springframework.web.socket.*
import java.net.*
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.springframework.web.socket.WebSocketSession as SpringWebSocketSession
import org.springframework.web.socket.client.WebSocketClient as SpringWebSocketClient

/**
 * Adapts this Spring [WebSocketClient][SpringWebSocketClient] to the Krossbow
 * [WebSocketClient][KrossbowWebSocketClient] interface.
 */
fun SpringWebSocketClient.asKrossbowWebSocketClient(): KrossbowWebSocketClient = SpringWebSocketClientAdapter(this)

private class SpringWebSocketClientAdapter(private val client: SpringWebSocketClient) : KrossbowWebSocketClient {

    override val supportsCustomHeaders: Boolean = true

    override suspend fun connect(url: String, headers: Map<String, String>): WebSocketConnectionWithPingPong {
        try {
            val handler = KrossbowToSpringHandlerAdapter()
            val handshakeHeaders = WebSocketHttpHeaders().apply {
                headers.forEach { (name, value) ->
                    put(name, listOf(value))
                }
            }
            val springSession = client.execute(handler, handshakeHeaders, URI(url)).await()
            return SpringSessionToKrossbowConnectionAdapter(springSession, handler.channelListener.incomingFrames)
        } catch (e: CancellationException) {
            throw e // this is an upstream exception that we don't want to wrap here
        } catch (e: Exception) {
            // javax.websocket.DeploymentException (when the handshake fails)
            //   Caused by DeploymentException (again, for some reason)
            //     Caused by:
            //      - java.nio.channels.UnresolvedAddressException (if host is not resolved)
            //      - org.glassfish.tyrus.client.auth.AuthenticationException: Authentication failed. (on 401)
            throw WebSocketConnectionException(
                url = url,
                httpStatusCode = null,
                additionalInfo = e.toString(),
                cause = e,
            )
        }
    }
}

// The StandardWebSocketClient uses jakarta.websocket.Endpoint behind the scenes as a handler, which guarantees the following:
// "Each instance of a websocket endpoint is guaranteed not to be called by more than one thread at a time per active connection"
// which means our handler adapter will not be called concurrently - no need for synchronization here.
private class KrossbowToSpringHandlerAdapter : WebSocketHandler {

    val channelListener: WebSocketListenerFlowAdapter = WebSocketListenerFlowAdapter()

    override fun afterConnectionEstablished(session: SpringWebSocketSession) {}

    override fun handleMessage(session: SpringWebSocketSession, message: WebSocketMessage<*>) {
        runBlocking {
            when (message) {
                is TextMessage -> channelListener.onTextMessage(message.payload, message.isLast)
                is BinaryMessage -> channelListener.onBinaryMessage(message.isLast) { write(message.payload) }
                is PingMessage -> channelListener.onPing(message.payload.readByteString())
                is PongMessage -> channelListener.onPong(message.payload.readByteString())
                else -> channelListener.onError("Unsupported Spring websocket message type: ${message.javaClass}")
            }
        }
    }

    override fun handleTransportError(session: SpringWebSocketSession, exception: Throwable) {
        channelListener.onError(exception)
    }

    override fun afterConnectionClosed(session: SpringWebSocketSession, closeStatus: CloseStatus) {
        // Note: afterConnectionClosed is synchronously called by Tyrus implementation during a session.close() call.
        // It is not called when receiving the server close frame if the closure is initiated on the client side.
        // Source: org.glassfish.tyrus.core.ProtocolHandler.close()
        // This means that if no receiver is listening on the incoming frames channel, onClose() here may suspend
        // forever (if the buffer is full).
        runBlocking {
            channelListener.onClose(closeStatus.code, closeStatus.reason)
        }
    }

    override fun supportsPartialMessages(): Boolean = true
}

private class SpringSessionToKrossbowConnectionAdapter(
    private val session: SpringWebSocketSession,
    override val incomingFrames: Flow<WebSocketFrame>,
) : WebSocketConnectionWithPingPong {

    // As per Spring documentation: "The underlying standard WebSocket session (JSR-356) does not allow concurrent
    // sending. Therefore, sending must be synchronized."
    private val mutex = Mutex()

    override val url: String
        get() = session.uri?.toString()!!

    override val canSend: Boolean
        get() = session.isOpen

    override suspend fun sendText(frameText: String) {
        mutex.withLock {
            session.sendMessage(TextMessage(frameText, true))
        }
    }

    override suspend fun sendBinary(frameData: ByteString) {
        mutex.withLock {
            session.sendMessage(BinaryMessage(frameData.asReadOnlyByteBuffer(), true))
        }
    }

    override suspend fun sendPing(frameData: ByteString) {
        mutex.withLock {
            session.sendMessage(PingMessage(frameData.asReadOnlyByteBuffer()))
        }
    }

    override suspend fun sendPong(frameData: ByteString) {
        mutex.withLock {
            session.sendMessage(PongMessage(frameData.asReadOnlyByteBuffer()))
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        mutex.withLock {
            session.close(CloseStatus(code, reason))
        }
    }
}
