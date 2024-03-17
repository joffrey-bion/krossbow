package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.java.*

class KtorJavaWebSocketClientTest : KtorClientTestSuite(supportsStatusCodes = true) {

    override fun provideEngine(): HttpClientEngineFactory<*> = Java
}
