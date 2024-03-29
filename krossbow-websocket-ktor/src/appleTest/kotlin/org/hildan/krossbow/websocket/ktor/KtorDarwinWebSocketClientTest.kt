package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

class KtorDarwinWebSocketClientTest : KtorClientTestSuite(supportsStatusCodes = false) {

    override fun provideEngine(): HttpClientEngineFactory<*> = Darwin
}
