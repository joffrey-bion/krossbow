package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*

class BuiltInWebSocketClientTest : WebSocketClientTestSuite(
    supportsStatusCodes = currentPlatform() !is Platform.Js,
    supportsCustomHeaders = currentPlatform() !is Platform.Js,
) {
    override fun provideClient() = WebSocketClient.builtIn()
}
