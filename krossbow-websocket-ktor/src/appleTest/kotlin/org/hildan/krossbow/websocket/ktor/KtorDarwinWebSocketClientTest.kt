package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

class KtorDarwinWebSocketClientTest : KtorClientTestSuite(
    supportsStatusCodes = false,
    // See https://youtrack.jetbrains.com/issue/KTOR-6970
    shouldTestNegotiatedSubprotocol = false,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Darwin
}
