package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*
import kotlin.time.Duration

abstract class KtorClientTestSuite(
    statusCodeSupport: StatusCodeSupport = StatusCodeSupport.All,
    shouldTestNegotiatedSubprotocol: Boolean = true,
    headersTestDelay: Duration? = null,
) : WebSocketClientTestSuite(statusCodeSupport, shouldTestNegotiatedSubprotocol, headersTestDelay) {

    override fun provideClient(): WebSocketClient = KtorWebSocketClient(
        HttpClient(provideEngine()) { install(WebSockets) },
    )

    abstract fun provideEngine(): HttpClientEngineFactory<*>
}
