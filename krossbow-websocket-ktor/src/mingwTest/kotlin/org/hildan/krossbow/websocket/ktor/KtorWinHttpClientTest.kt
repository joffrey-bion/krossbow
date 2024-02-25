package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.winhttp.*

class KtorWinHttpClientTest : KtorClientTestSuite(supportsStatusCodes = false, supportsCustomHeaders = true) {

    override fun provideEngine(): HttpClientEngineFactory<*> = WinHttp
}
