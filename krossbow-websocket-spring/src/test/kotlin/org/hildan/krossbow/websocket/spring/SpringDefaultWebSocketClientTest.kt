package org.hildan.krossbow.websocket.spring

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class SpringDefaultWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = SpringDefaultWebSocketClient
}
