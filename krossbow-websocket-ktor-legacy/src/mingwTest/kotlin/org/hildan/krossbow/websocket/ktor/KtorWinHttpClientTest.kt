package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.winhttp.*
import org.hildan.krossbow.websocket.test.*

class KtorWinHttpClientTest : KtorClientTestSuite(
    // WinHttp: error is too generic and doesn't differ per status code
    statusCodeSupport = StatusCodeSupport.None,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = WinHttp
}
