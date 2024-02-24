package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

class KtorCioWebSocketClientTest : KtorClientTestSuite() {

    override fun provideEngine(): HttpClientEngineFactory<*> = CIO
}
