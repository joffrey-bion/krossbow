package org.hildan.krossbow.websocket.ktor

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.*

private val Platform.supportsStatusCodes: Boolean
    get() = this !is Platform.Windows

class KtorWebSocketClientTest : WebSocketClientTestSuite(
    supportsStatusCodes = currentPlatform().supportsStatusCodes,
    supportsCustomHeaders = currentPlatform() !is Platform.Js.Browser,
) {

    override fun provideClient(): WebSocketClient = KtorWebSocketClient()
}
