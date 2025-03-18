package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import org.hildan.krossbow.websocket.test.*

class KtorWasmJsWebSocketClientTest : KtorClientTestSuite(
    // The browser cannot reveal status codes for security reasons
    statusCodeSupport = if (currentPlatform() is Platform.WasmJs.Browser) StatusCodeSupport.None else StatusCodeSupport.All,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Js
}
