package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import org.hildan.krossbow.websocket.test.StatusCodeSupport

class KtorDarwinWebSocketClientTest : KtorClientTestSuite(
    statusCodeSupport = StatusCodeSupport.None,
    // See https://youtrack.jetbrains.com/issue/KTOR-6970
    shouldTestNegotiatedSubprotocol = false,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Darwin
}
