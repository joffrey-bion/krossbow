package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.curl.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.experimental.*

@OptIn(ExperimentalNativeApi::class)
class KtorCurlWebSocketAutobahnTest : AutobahnClientTestSuite(
    agentUnderTest = "krossbow-ktor-curl-client-${Platform.osFamily.name.lowercase()}",
) {
    override fun provideClient(): WebSocketClient = KtorWebSocketClient(HttpClient(Curl) { install(WebSockets) })
}
