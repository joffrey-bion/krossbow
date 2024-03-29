package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import org.hildan.krossbow.websocket.test.*
import kotlin.time.Duration.Companion.milliseconds

class KtorJsWebSocketClientTest : KtorClientTestSuite(
    // JS node: error is too generic and doesn't differ per status code (ECONNREFUSED, unlike 'ws')
    // JS browser: cannot support status codes for security reasons
    supportsStatusCodes = false,
    // workaround for https://youtrack.jetbrains.com/issue/KTOR-6883
    headersTestDelay = 200.milliseconds.takeIf { currentPlatform() == Platform.Js.NodeJs },
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Js
}
