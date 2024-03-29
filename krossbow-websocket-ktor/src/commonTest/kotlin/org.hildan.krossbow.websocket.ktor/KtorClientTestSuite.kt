package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*
import kotlin.time.*

abstract class KtorClientTestSuite(
    supportsStatusCodes: Boolean,
    headersTestDelay: Duration? = null,
) : WebSocketClientTestSuite(supportsStatusCodes, headersTestDelay) {

    override fun provideClient(): WebSocketClient = KtorWebSocketClient(
        HttpClient(provideEngine()) { install(WebSockets) },
    )

    abstract fun provideEngine(): HttpClientEngineFactory<*>
}
