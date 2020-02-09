package org.hildan.krossbow.websocket.spring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hildan.krossbow.websocket.KWebSocketClient
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.KWebSocketSession
import org.hildan.krossbow.websocket.NoopWebsocketListener
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.PingMessage
import org.springframework.web.socket.PongMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object SpringDefaultWebSocketClient: SpringWebSocketClientAdapter(StandardWebSocketClient())

object SpringSockJSWebSocketClient: SpringWebSocketClientAdapter(SockJsClient(defaultWsTransports()))

private fun defaultWsTransports(): List<Transport> = listOf(WebSocketTransport(StandardWebSocketClient()))

open class SpringWebSocketClientAdapter(private val client: WebSocketClient): KWebSocketClient {

    override suspend fun connect(url: String): KWebSocketSession {
        return suspendCoroutine { cont ->
            val handler = object : WebSocketHandler {

                private var WebSocketSession.krossbowWrapper: KWebSocketSession
                    get() = attributes["krossbowSession"] as KWebSocketSession
                    set(value) { attributes["krossbowSession"] = value }

                override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
                    runBlocking {
                        session.krossbowWrapper.listener.onError(exception)
                    }
                }

                override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
                    runBlocking {
                        session.krossbowWrapper.listener.onClose()
                    }
                }

                override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
                    val listener = session.krossbowWrapper.listener
                    runBlocking {
                        when(message) {
                            is PingMessage -> Unit
                            is PongMessage -> Unit
                            is BinaryMessage -> listener.onBinaryMessage(message.payload.array())
                            is TextMessage -> listener.onTextMessage(message.payload)
                            else -> error("Unsupported Spring websocket message type: ${message.javaClass}")
                        }
                    }
                }

                override fun afterConnectionEstablished(session: WebSocketSession) {
                    val krossbowSession = SpringWebSocketSession(session)
                    session.krossbowWrapper = krossbowSession
                    cont.resume(krossbowSession)
                }

                override fun supportsPartialMessages(): Boolean = false
            }
            client.doHandshake(handler, url)
        }
    }
}

class SpringWebSocketSession(private val session: WebSocketSession): KWebSocketSession {
    override var listener: KWebSocketListener = NoopWebsocketListener

    override suspend fun send(frameData: ByteArray) {
        withContext(Dispatchers.IO) {
            session.sendMessage(BinaryMessage(frameData))
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            session.close()
        }
    }
}
