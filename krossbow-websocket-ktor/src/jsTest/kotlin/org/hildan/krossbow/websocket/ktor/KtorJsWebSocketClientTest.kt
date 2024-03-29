package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

class KtorJsWebSocketClientTest : KtorClientTestSuite(
    // JS node: error is too generic and doesn't differ per status code (ECONNREFUSED, unlike 'ws')
    // JS browser: cannot support status codes for security reasons
    supportsStatusCodes = false,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Js
}
