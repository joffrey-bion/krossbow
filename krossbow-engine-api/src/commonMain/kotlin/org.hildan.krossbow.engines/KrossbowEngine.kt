package org.hildan.krossbow.engines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel

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

interface KrossbowSession {

    suspend fun send(destination: String, body: Any)

    suspend fun <T> subscribe(destination: String, onFrameReceived: (T) -> Unit)
    suspend fun <T> CoroutineScope.subscribe(destination: String): ReceiveChannel<T>

    suspend fun disconnect()
}
