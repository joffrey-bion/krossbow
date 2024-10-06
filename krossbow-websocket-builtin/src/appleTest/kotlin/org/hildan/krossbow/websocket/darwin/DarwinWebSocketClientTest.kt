package org.hildan.krossbow.websocket.darwin

import org.hildan.krossbow.websocket.test.*

class DarwinWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient() = DarwinWebSocketClient()
}
