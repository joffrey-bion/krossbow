package org.hildan.krossbow.websocket.js

import IsomorphicWebSocket
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.autobahn.AutobahnClientTestSuite
import org.hildan.krossbow.websocket.test.isBrowser

class JsWebSocketAutobahnTest : AutobahnClientTestSuite("krossbow-js-client-${environment()}") {

    override fun provideClient(): WebSocketClient = JsWebSocketClientAdapter { url -> IsomorphicWebSocket(url) }
}

private fun environment() = if (isBrowser()) "browser" else "nodejs"
