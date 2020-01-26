package org.hildan.krossbow.engines.mpp

import io.ktor.util.KtorExperimentalAPI
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowEngineClient
import org.hildan.krossbow.engines.KrossbowEngineConfig
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.SubscriptionCallbacks
import org.hildan.krossbow.engines.mpp.websocket.KtorWebSocket
import org.hildan.krossbow.engines.mpp.websocket.WebSocket
import org.hildan.krossbow.engines.mpp.websocket.WebSocketSession

object MppKrossbowEngine: KrossbowEngine {

    override fun createClient(config: KrossbowEngineConfig): KrossbowEngineClient =
            MppKrossbowEngineClient(config, KtorWebSocket())
}

@UseExperimental(KtorExperimentalAPI::class)
class MppKrossbowEngineClient(
    private val config: KrossbowEngineConfig,
    private val webSocket: WebSocket
): KrossbowEngineClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowEngineSession =
            MppKrossbowEngineSession(config, webSocket.connect(url))
}

class MppKrossbowEngineSession(
    private val config: KrossbowEngineConfig,
    private val webSocketSession: WebSocketSession
): KrossbowEngineSession {

    override suspend fun send(destination: String, body: ByteArray?): KrossbowReceipt? {
        TODO()
    }

    override suspend fun subscribe(destination: String, callbacks: SubscriptionCallbacks<ByteArray>): KrossbowEngineSubscription {
        TODO()
    }

    override suspend fun subscribeNoPayload(destination: String, callbacks: SubscriptionCallbacks<Unit>): KrossbowEngineSubscription {
        TODO()
    }

    override suspend fun disconnect() {
        TODO()
    }
}
