package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import org.hildan.krossbow.websocket.test.*

class KtorJsWebSocketClientTest : KtorClientTestSuite(
    // JS browser: cannot support status codes for security reasons
    supportsStatusCodes = currentJsPlatform() !is Platform.Js.Browser,
    // See https://youtrack.jetbrains.com/issue/KTOR-6970
    shouldTestNegotiatedSubprotocol = false,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Js
}
