package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*

class BuiltInWebSocketClientTest : WebSocketClientTestSuite(
    statusCodeSupport = if (currentPlatform() is Platform.Js.Browser) StatusCodeSupport.None else StatusCodeSupport.All,
) {
    override fun provideClient() = WebSocketClient.builtIn()
}
