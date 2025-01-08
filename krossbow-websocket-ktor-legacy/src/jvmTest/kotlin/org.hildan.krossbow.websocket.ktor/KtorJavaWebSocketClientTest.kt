package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.java.*

class KtorJavaWebSocketClientTest : KtorClientTestSuite(
    // See https://youtrack.jetbrains.com/issue/KTOR-6970
    shouldTestNegotiatedSubprotocol = false,
) {

    override fun provideEngine(): HttpClientEngineFactory<*> = Java
}
