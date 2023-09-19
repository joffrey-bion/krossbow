package org.hildan.krossbow.websocket.spring

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite
import org.springframework.web.socket.client.standard.StandardWebSocketClient

class SpringDefaultWebSocketClientTest : WebSocketClientTestSuite(supportsStatusCodes = false) {

    override fun provideClient(): WebSocketClient = StandardWebSocketClient().asKrossbowWebSocketClient()
}
