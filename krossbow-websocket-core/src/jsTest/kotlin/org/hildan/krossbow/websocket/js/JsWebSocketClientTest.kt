package org.hildan.krossbow.websocket.js

import IsomorphicJsWebSocketClient
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class JsWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient() = IsomorphicJsWebSocketClient
}
