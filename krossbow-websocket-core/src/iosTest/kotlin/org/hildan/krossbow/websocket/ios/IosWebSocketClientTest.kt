package org.hildan.krossbow.websocket.ios

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.default
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class IosWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient() = WebSocketClient.default()
}
