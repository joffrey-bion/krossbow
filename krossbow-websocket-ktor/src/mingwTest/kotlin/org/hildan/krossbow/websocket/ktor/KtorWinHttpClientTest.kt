package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.winhttp.*

class KtorWinHttpClientTest : KtorClientTestSuite(
    // WinHttp: error is too generic and doesn't differ per status code
    supportsStatusCodes = false,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = WinHttp
}
