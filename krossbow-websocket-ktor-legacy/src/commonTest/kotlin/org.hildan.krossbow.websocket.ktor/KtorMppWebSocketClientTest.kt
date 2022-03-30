package org.hildan.krossbow.websocket.ktor

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class KtorWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = KtorLegacyWebSocketClient()
}
