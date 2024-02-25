package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import org.hildan.krossbow.websocket.test.*

class KtorJsWebSocketClientTest : KtorClientTestSuite(
    supportsStatusCodes = false,
    supportsCustomHeaders = currentJsPlatform() == Platform.Js.NodeJs,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Js
}
