package org.hildan.krossbow.engines.spring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowSession
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.*
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
            val futureSession = client.connect(url, NoopStompSessionHandler)
            cont.resume(SpringKrossbowSession(futureSession.get()))
        }
    }
}

private object NoopStompSessionHandler: StompSessionHandlerAdapter()

private class SpringKrossbowSession(private val session: StompSession): KrossbowSession {

    override suspend fun send(destination: String, body: Any) {
        TODO("send not implemented")
    }

    override suspend fun <T> subscribe(destination: String, onFrameReceived: (T) -> Unit) {
        TODO("subscribe not implemented")
    }

    override suspend fun <T> CoroutineScope.subscribe(destination: String): ReceiveChannel<T> {
        TODO("subscribe (channel) not implemented")
    }

    override suspend fun disconnect() {
        TODO("disconnect not implemented")
    }
}
