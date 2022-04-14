package org.hildan.krossbow.websocket.test.autobahn

import IsomorphicJsWebSocketClient
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.environment

class JsWebSocketAutobahnTest : AutobahnClientTestSuite("krossbow-js-client-${environment()}") {

    override fun provideClient(): WebSocketClient = IsomorphicJsWebSocketClient
}
