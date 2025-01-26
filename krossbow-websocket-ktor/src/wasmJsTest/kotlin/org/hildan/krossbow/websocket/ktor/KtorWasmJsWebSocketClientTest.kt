package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import org.hildan.krossbow.websocket.test.*
import kotlin.time.Duration.Companion.milliseconds

class KtorWasmJsWebSocketClientTest : KtorClientTestSuite(
    // The browser cannot reveal status codes for security reasons
    statusCodeSupport = if (currentPlatform() is Platform.WasmJs.Browser) StatusCodeSupport.None else StatusCodeSupport.All,
    // See https://youtrack.jetbrains.com/issue/KTOR-6970
    shouldTestNegotiatedSubprotocol = false,
    // workaround for https://youtrack.jetbrains.com/issue/KTOR-6883 (NOT fixed for WASM)
    headersTestDelay = 200.milliseconds.takeIf { currentPlatform() == Platform.WasmJs.NodeJs },
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Js
}
