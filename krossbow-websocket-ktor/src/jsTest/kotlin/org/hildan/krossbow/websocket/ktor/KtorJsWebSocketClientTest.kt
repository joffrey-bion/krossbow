package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

class KtorJsWebSocketClientTest : KtorClientTestSuite(
    supportsStatusCodes = false,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Js
}
