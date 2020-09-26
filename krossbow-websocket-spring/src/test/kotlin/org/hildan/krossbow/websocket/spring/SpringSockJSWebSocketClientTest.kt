package org.hildan.krossbow.websocket.spring

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite
import kotlin.test.Ignore

@Ignore // requires a SockJS server
class SpringSockJSWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = SpringSockJSWebSocketClient
}
