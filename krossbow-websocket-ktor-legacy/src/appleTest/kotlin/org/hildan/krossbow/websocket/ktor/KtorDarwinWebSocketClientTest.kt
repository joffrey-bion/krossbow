package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import kotlinx.coroutines.test.*
import org.hildan.krossbow.websocket.test.*
import kotlin.test.*

class KtorDarwinWebSocketClientTest : KtorClientTestSuite(
    statusCodeSupport = StatusCodeSupport.None,
    // See https://youtrack.jetbrains.com/issue/KTOR-6970
    shouldTestNegotiatedSubprotocol = false,
) {
    override fun provideEngine(): HttpClientEngineFactory<*> = Darwin

    // disabled because of unexplained JobCancellationException on close
    // https://github.com/joffrey-bion/krossbow/issues/522
    @Test
    override fun testEchoText(): TestResult {}
    @Test
    override fun testEchoBinary(): TestResult {}
}
