package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.winhttp.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.experimental.*

@OptIn(ExperimentalNativeApi::class)
class KtorWinHttpWebSocketAutobahnTest : AutobahnClientTestSuite(
    agentUnderTest = "krossbow-ktor-winhttp-client-${Platform.osFamily.name.lowercase()}",
    exclusions = listOf(
        CaseExclusion(
            caseIdPrefixes = listOf("2.10", "2.11"),
            reason = "Autobahn ping-pong tests are stricter than the spec. The Ktor client sometimes just sends the " +
                "last PONG, which is conform to the spec and should be accepted by the test.",
        ),
    ),
) {
    override fun provideClient(): WebSocketClient = KtorWebSocketClient(HttpClient(WinHttp) { install(WebSockets) })
}
