package org.hildan.krossbow.websocket.spring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hildan.krossbow.websocket.NoopWebSocketListener
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
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.hildan.krossbow.websocket.WebSocketListener as KrossbowWebSocketListener
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

    override suspend fun connect(url: String): KrossbowWebSocketSession =
            client.doHandshake(KrossbowToSpringHandlerAdapter, url).completable().await().krossbowWrapper
}

private var SpringWebSocketSession.krossbowWrapper: KrossbowWebSocketSession
    get() = attributes["krossbowSession"] as KrossbowWebSocketSession
    set(value) {
        attributes["krossbowSession"] = value
    }

object KrossbowToSpringHandlerAdapter : WebSocketHandler {

    override fun afterConnectionEstablished(session: SpringWebSocketSession) {
        session.krossbowWrapper = SpringToKrossbowSessionAdapter(session)
    }

    override fun handleMessage(session: SpringWebSocketSession, message: WebSocketMessage<*>) {
        val listener = session.krossbowWrapper.listener
        runBlocking {
            when (message) {
                is PingMessage -> Unit
                is PongMessage -> Unit
                is BinaryMessage -> listener.onBinaryMessage(message.payload.array())
                is TextMessage -> listener.onTextMessage(message.payload)
                else -> error("Unsupported Spring websocket message type: ${message.javaClass}")
            }
        }
    }

    override fun handleTransportError(session: SpringWebSocketSession, exception: Throwable) {
        runBlocking {
            session.krossbowWrapper.listener.onError(exception)
        }
    }

    override fun afterConnectionClosed(session: SpringWebSocketSession, closeStatus: CloseStatus) {
        runBlocking {
            session.krossbowWrapper.listener.onClose(closeStatus.code, closeStatus.reason)
        }
    }

    override fun supportsPartialMessages(): Boolean = false
}

class SpringToKrossbowSessionAdapter(private val session: SpringWebSocketSession) : KrossbowWebSocketSession {

    override var listener: KrossbowWebSocketListener = NoopWebSocketListener

    override suspend fun sendText(frameText: String) {
        withContext(Dispatchers.IO) {
            session.sendMessage(TextMessage(frameText, true))
        }
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        withContext(Dispatchers.IO) {
            session.sendMessage(BinaryMessage(frameData, true))
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        withContext(Dispatchers.IO) {
            session.close(CloseStatus(code, reason))
        }
    }
}

// FIXME Use a single coroutine or a concurrent wrapper for sending WS frames with Spring.
//  From Spring docs on sendMessage():
//  The underlying standard WebSocket session (JSR-356) does not allow concurrent sending.
//  Therefore sending must be synchronized.
//  To ensure that, one option is to wrap the WebSocketSession with the ConcurrentWebSocketSessionDecorator.
