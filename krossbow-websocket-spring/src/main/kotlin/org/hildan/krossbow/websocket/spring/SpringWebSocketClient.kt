package org.hildan.krossbow.websocket.spring

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import org.hildan.krossbow.websocket.WebSocketConnectionWithPingPong
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.PingMessage
import org.springframework.web.socket.PongMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.nio.ByteBuffer
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.springframework.web.socket.WebSocketSession as SpringWebSocketSession
import org.springframework.web.socket.client.WebSocketClient as SpringWebSocketClient

object SpringDefaultWebSocketClient : SpringWebSocketClientAdapter(StandardWebSocketClient())

object SpringSockJSWebSocketClient : SpringWebSocketClientAdapter(SockJsClient(defaultWsTransports()))

private fun defaultWsTransports(): List<Transport> = listOf(
    WebSocketTransport(StandardWebSocketClient()),
    RestTemplateXhrTransport()
)

open class SpringWebSocketClientAdapter(private val client: SpringWebSocketClient) : KrossbowWebSocketClient {

    override suspend fun connect(url: String): WebSocketConnectionWithPingPong {
        val handler = KrossbowToSpringHandlerAdapter()
        val springSession = client.doHandshake(handler, url).completable().await()
        return SpringSessionToKrossbowConnectionAdapter(springSession, handler.channelListener.incomingFrames)
    }
}

private class KrossbowToSpringHandlerAdapter : WebSocketHandler {

    val channelListener: WebSocketListenerChannelAdapter = WebSocketListenerChannelAdapter()

    override fun afterConnectionEstablished(session: SpringWebSocketSession) {}

    override fun handleMessage(session: SpringWebSocketSession, message: WebSocketMessage<*>) {
        runBlocking {
            when (message) {
                is TextMessage -> channelListener.onTextMessage(message.payload, message.isLast)
                is BinaryMessage -> channelListener.onBinaryMessage(message.payload.array(), message.isLast)
                is PingMessage -> channelListener.onPing(message.payload.array())
                is PongMessage -> channelListener.onPong(message.payload.array())
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
        // This means that if no receiver is listening on the incoming frames channel, onClose() here may suspend forever
        runBlocking {
            channelListener.onClose(closeStatus.code, closeStatus.reason)
        }
    }

    override fun supportsPartialMessages(): Boolean = true
}

@Suppress("BlockingMethodInNonBlockingContext")
private class SpringSessionToKrossbowConnectionAdapter(
    private val session: SpringWebSocketSession,
    override val incomingFrames: ReceiveChannel<WebSocketFrame>
) : WebSocketConnectionWithPingPong {

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

    override suspend fun sendBinary(frameData: ByteArray) {
        mutex.withLock {
            session.sendMessage(BinaryMessage(frameData, true))
        }
    }

    override suspend fun sendPing(frameData: ByteArray) {
        mutex.withLock {
            session.sendMessage(PingMessage(ByteBuffer.wrap(frameData)))
        }
    }

    override suspend fun sendPong(frameData: ByteArray) {
        mutex.withLock {
            session.sendMessage(PongMessage(ByteBuffer.wrap(frameData)))
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        mutex.withLock {
            session.close(CloseStatus(code, reason))
        }
    }
}
