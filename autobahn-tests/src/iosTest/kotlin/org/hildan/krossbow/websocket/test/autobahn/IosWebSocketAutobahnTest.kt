package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.ios.IosWebSocketClient

class IosWebSocketAutobahnTest : AutobahnClientTestSuite("krossbow-ios-client") {

    override fun provideClient(): WebSocketClient = IosWebSocketClient()
}
