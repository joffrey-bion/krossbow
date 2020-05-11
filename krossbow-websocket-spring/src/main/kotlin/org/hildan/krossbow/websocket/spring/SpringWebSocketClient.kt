package org.hildan.krossbow.websocket.spring

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
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
import java.util.concurrent.Executors
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.hildan.krossbow.websocket.WebSocketSession as KrossbowWebSocketSession
import org.springframework.web.socket.WebSocketSession as SpringWebSocketSession
import org.springframework.web.socket.client.WebSocketClient as SpringWebSocketClient

object SpringDefaultWebSocketClient : SpringWebSocketClientAdapter(StandardWebSocketClient())

object SpringSockJSWebSocketClient : SpringWebSocketClientAdapter(SockJsClient(defaultWsTransports()))

private fun defaultWsTransports(): List<Transport> = listOf(
    WebSocketTransport(StandardWebSocketClient()),
    RestTemplateXhrTransport()
)

open class SpringWebSocketClientAdapter(private val client: SpringWebSocketClient) : KrossbowWebSocketClient {

    override suspend fun connect(url: String): KrossbowWebSocketSession {
        val handler = KrossbowToSpringHandlerAdapter()
        val springSession = client.doHandshake(handler, url).completable().await()
        return SpringToKrossbowSessionAdapter(springSession, handler.channelListener.incomingFrames)
    }
}

private class KrossbowToSpringHandlerAdapter : WebSocketHandler {

    val channelListener: WebSocketListenerChannelAdapter = WebSocketListenerChannelAdapter()

    override fun afterConnectionEstablished(session: SpringWebSocketSession) {}

    override fun handleMessage(session: SpringWebSocketSession, message: WebSocketMessage<*>) {
        runBlocking {
            when (message) {
                is PingMessage -> Unit
                is PongMessage -> Unit
                is BinaryMessage -> channelListener.onBinaryMessage(message.payload.array(), message.isLast)
                is TextMessage -> channelListener.onTextMessage(message.payload, message.isLast)
                else -> channelListener.onError("Unsupported Spring websocket message type: ${message.javaClass}")
            }
        }
    }

    override fun handleTransportError(session: SpringWebSocketSession, exception: Throwable) {
        channelListener.onError(exception)
    }

    override fun afterConnectionClosed(session: SpringWebSocketSession, closeStatus: CloseStatus) {
        runBlocking {
            channelListener.onClose(closeStatus.code, closeStatus.reason)
        }
    }

    override fun supportsPartialMessages(): Boolean = true
}

private class SpringToKrossbowSessionAdapter(
    private val session: SpringWebSocketSession,
    override val incomingFrames: ReceiveChannel<WebSocketFrame>
) : KrossbowWebSocketSession {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override val canSend: Boolean
        get() = session.isOpen

    override suspend fun sendText(frameText: String) {
        withContext(dispatcher) {
            session.sendMessage(TextMessage(frameText, true))
        }
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        withContext(dispatcher) {
            session.sendMessage(BinaryMessage(frameData, true))
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        withContext(dispatcher) {
            session.close(CloseStatus(code, reason))
        }
        dispatcher.close()
    }
}
