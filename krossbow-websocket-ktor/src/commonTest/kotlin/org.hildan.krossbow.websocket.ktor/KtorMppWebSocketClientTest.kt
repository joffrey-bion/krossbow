package org.hildan.krossbow.websocket.ktor

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.*

// TODO should rather be based on Ktor engines, not platforms
//   Currently the other platforms use CIO because of classpath order, and CIO supports status codes.
private val Platform.supportsStatusCodes: Boolean
    get() = this !is Platform.Windows && this !is Platform.Js.Browser

class KtorMppWebSocketClientTest : WebSocketClientTestSuite(
    supportsStatusCodes = currentPlatform().supportsStatusCodes,
    supportsCustomHeaders = currentPlatform() !is Platform.Js.Browser,
) {

    override fun provideClient(): WebSocketClient = KtorWebSocketClient()
}
