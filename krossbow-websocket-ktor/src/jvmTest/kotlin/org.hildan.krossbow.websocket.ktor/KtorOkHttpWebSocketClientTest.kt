package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

class KtorOkHttpWebSocketClientTest : KtorClientTestSuite() {

    override fun provideEngine(): HttpClientEngineFactory<*> = OkHttp
}
