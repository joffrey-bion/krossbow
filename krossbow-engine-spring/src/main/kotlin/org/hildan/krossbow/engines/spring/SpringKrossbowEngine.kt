package org.hildan.krossbow.engines.spring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowSession
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object SpringKrossbowEngine: KrossbowEngine {

    override fun createClient(config: KrossbowConfig): KrossbowClient {
        return SpringKrossbowClient(defaultStompClient())
    }

    private fun defaultWsTransports(): List<Transport> = listOf<Transport>(WebSocketTransport(StandardWebSocketClient()))

    private fun defaultWsClient(transports: List<Transport> = defaultWsTransports()): WebSocketClient = SockJsClient(transports)

    private fun defaultStompClient(webSocketClient: WebSocketClient = defaultWsClient()): WebSocketStompClient =
        WebSocketStompClient(webSocketClient).apply {
            messageConverter = MappingJackson2MessageConverter() // for custom object exchanges
            taskScheduler = createTaskScheduler() // for heartbeats
        }

    private fun createTaskScheduler(): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply { afterPropertiesSet() }

}

class SpringKrossbowClient(private val client: WebSocketStompClient): KrossbowClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowSession {
        return suspendCoroutine { cont ->
            val futureSession = client.connect(url, object : StompSessionHandler {
                override fun handleException(
                    session: StompSession?,
                    command: StompCommand?,
                    headers: StompHeaders?,
                    payload: ByteArray?,
                    exception: Throwable?
                ) {
                    cont.resumeWithException(exception!!)
                }

                override fun handleTransportError(session: StompSession?, exception: Throwable?) {
                    cont.resumeWithException(exception!!)
                }

                override fun handleFrame(headers: StompHeaders?, payload: Any?): Unit = TODO("not implemented")

                override fun afterConnected(session: StompSession?, connectedHeaders: StompHeaders?): Unit = TODO("not implemented")

                override fun getPayloadType(headers: StompHeaders?): Type = TODO("not implemented")
            })
            cont.resume(SpringKrossbowSession(futureSession.get()))
        }
    }
}

private class SpringKrossbowSession(private val session: StompSession): KrossbowSession {

    override suspend fun send(destination: String, body: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun <T> subscribe(destination: String, onFrameReceived: (T) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun <T> CoroutineScope.subscribe(destination: String): ReceiveChannel<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun disconnect() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
