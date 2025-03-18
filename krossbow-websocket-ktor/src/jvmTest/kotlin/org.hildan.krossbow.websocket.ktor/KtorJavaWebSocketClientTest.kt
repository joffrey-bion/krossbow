package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.java.*

class KtorJavaWebSocketClientTest : KtorClientTestSuite() {

    override fun provideEngine(): HttpClientEngineFactory<*> = Java
}
