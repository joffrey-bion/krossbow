package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import kotlinx.coroutines.test.TestResult
import org.hildan.krossbow.websocket.test.StatusCodeSupport

class KtorDarwinWebSocketClientTest : KtorClientTestSuite(
    statusCodeSupport = StatusCodeSupport.None,
    // See https://youtrack.jetbrains.com/issue/KTOR-6970
    shouldTestNegotiatedSubprotocol = false,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Darwin

    // disabled because of unexplained JobCancellationException on close
    // https://github.com/joffrey-bion/krossbow/issues/522
    override fun testEchoText(): TestResult {}
    override fun testEchoBinary(): TestResult {}
}
