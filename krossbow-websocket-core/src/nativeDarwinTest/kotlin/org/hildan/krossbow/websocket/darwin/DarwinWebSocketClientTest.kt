package org.hildan.krossbow.websocket.darwin

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.default
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class DarwinWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient() = WebSocketClient.default()
}
