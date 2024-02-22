package org.hildan.krossbow.websocket.js

import org.hildan.krossbow.websocket.test.*

class JsWebSocketClientTest : WebSocketClientTestSuite(supportsStatusCodes = false) {

    override fun provideClient() = BrowserWebSocketClient
}
