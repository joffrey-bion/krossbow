package org.hildan.krossbow.websocket.test

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.js.BrowserWebSocketClient
import kotlin.test.Ignore

// TODO provide WebSocket for node somehow (using isomorphic-ws package?)
@Ignore
class JsWebSocketTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = BrowserWebSocketClient
}
