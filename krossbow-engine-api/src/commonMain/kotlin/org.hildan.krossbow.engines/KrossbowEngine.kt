package org.hildan.krossbow.engines

import kotlin.reflect.KClass

@Suppress("FunctionName")
fun KrossbowClient(engine: KrossbowEngine, configure: KrossbowConfig.() -> Unit = {}): KrossbowClient {
    val config = KrossbowConfig().apply { configure() }
    return engine.createClient(config)
}

interface KrossbowEngine {

    fun createClient(config: KrossbowConfig): KrossbowClient
}

interface KrossbowClient {

    /**
     * Connects to the given WebSocket [url] and to the STOMP session, and returns after receiving the CONNECTED frame.
     */
    suspend fun connect(url: String, login: String? = null, passcode: String? = null): KrossbowSession
}

suspend fun KrossbowClient.useSession(
    url: String,
    login: String? = null,
    passcode: String? = null,
    block: suspend KrossbowSession.() -> Unit
) {
    val session = connect(url, login, passcode)
    try {
        session.block()
    } finally {
        session.disconnect()
    }
}

interface KrossbowEngineSession {

    suspend fun send(destination: String, body: Any): KrossbowReceipt?

    suspend fun <T : Any> subscribe(
        destination: String,
        clazz: KClass<T>,
        callbacks: SubscriptionCallbacks<T>
    ): KrossbowEngineSubscription

    suspend fun disconnect()
}

data class KrossbowEngineSubscription(
    val id: String,
    val unsubscribe: suspend (UnsubscribeHeaders?) -> Unit
)
