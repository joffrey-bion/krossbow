package org.hildan.krossbow.websocket.js

import org.hildan.krossbow.websocket.test.*

class JsBrowserWebSocketClientTest : WebSocketClientTestSuite(statusCodeSupport = StatusCodeSupport.None) {

    override fun provideClient() = BrowserWebSocketClient
}
