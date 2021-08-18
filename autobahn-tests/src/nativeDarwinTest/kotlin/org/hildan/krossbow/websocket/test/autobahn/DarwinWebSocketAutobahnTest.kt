package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.darwin.DarwinWebSocketClient

class DarwinWebSocketAutobahnTest : AutobahnClientTestSuite("krossbow-darwin-client") {

    override fun provideClient(): WebSocketClient = DarwinWebSocketClient()
}
