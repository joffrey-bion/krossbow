package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.ios.IosWebSocketClient
import kotlin.test.Ignore

@Ignore // FIXME strange issues with close frames - to investigate
class IosWebSocketAutobahnTest : AutobahnClientTestSuite("krossbow-ios-client") {

    override fun provideClient(): WebSocketClient = IosWebSocketClient()
}
