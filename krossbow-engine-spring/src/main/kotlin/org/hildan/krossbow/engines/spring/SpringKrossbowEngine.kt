package org.hildan.krossbow.engines.spring

import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.KrossbowSession
import org.hildan.krossbow.engines.LostReceiptException
import org.hildan.krossbow.engines.SubscriptionCallbacks
import org.hildan.krossbow.engines.UnsubscribeHeaders
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

/**
 * Implementation of [KrossbowEngine] for the JVM based on Spring's [WebSocketStompClient].
 */
object SpringKrossbowEngine : KrossbowEngine {

    override fun createClient(config: KrossbowConfig): KrossbowClient {
        return SpringKrossbowClient(defaultStompClient())
    }

    private fun defaultStompClient(webSocketClient: WebSocketClient = defaultWsClient()): WebSocketStompClient =
        WebSocketStompClient(webSocketClient).apply {
            messageConverter = createJacksonConverter() // for custom object exchanges
            taskScheduler = createTaskScheduler() // for heartbeats
        }

    private fun createJacksonConverter() = MappingJackson2MessageConverter().apply {
        objectMapper.registerModule(KotlinModule())
    }

    private fun defaultWsClient(transports: List<Transport> = defaultWsTransports()): WebSocketClient =
        SockJsClient(transports)

    private fun defaultWsTransports(): List<Transport> =
        listOf<Transport>(WebSocketTransport(StandardWebSocketClient()))

    private fun createTaskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply { afterPropertiesSet() }
}

private class SpringKrossbowClient(private val client: WebSocketStompClient) : KrossbowClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowSession {
        val sessionHandler = LoggingStompSessionHandler()
        val session = client.connect(url, sessionHandler).completable().await()
        val engineSession = SpringKrossbowSession(session, sessionHandler)
        return KrossbowSession(engineSession)
    }
}

private class SpringKrossbowSession(
    private val session: StompSession,
    private val sessionHandler: LoggingStompSessionHandler
) : KrossbowEngineSession {

    init {
        session.setAutoReceipt(true)
    }

    override suspend fun send(destination: String, body: Any?): KrossbowReceipt? =
        session.send(destination, body).await()?.let { KrossbowReceipt(it) }

    override suspend fun <T : Any> subscribe(
        destination: String,
        clazz: KClass<T>,
        callbacks: SubscriptionCallbacks<T>
    ): KrossbowEngineSubscription {
        val handler = SingleTypeFrameHandler(clazz) {
            runBlocking {
                callbacks.onReceive(it)
            }
        }
        val sub = session.subscribe(destination, handler)
        sessionHandler.registerExceptionHandler(sub.subscriptionId!!) {
            callbacks.onError(it)
        }

        return KrossbowEngineSubscription(sub.subscriptionId!!) { headers ->
            sub.unsubscribe(headers?.toSpringStompHeaders())
        }
    }

    override suspend fun disconnect() {
        session.disconnect()
    }
}

private fun UnsubscribeHeaders.toSpringStompHeaders(): StompHeaders = StompHeaders() // TODO fill this up

private suspend fun StompSession.Receiptable.await(): String? = if (receiptId == null) {
    // no receipt mechanism used, so we return immediately
    null
} else suspendCoroutine { cont ->
    addReceiptTask { cont.resume(receiptId!!) }
    addReceiptLostTask { cont.resumeWithException(LostReceiptException(receiptId!!)) }
}
