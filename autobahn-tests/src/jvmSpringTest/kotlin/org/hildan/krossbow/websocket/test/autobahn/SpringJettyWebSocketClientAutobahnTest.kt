package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.spring.asKrossbowWebSocketClient
import org.springframework.web.socket.client.jetty.JettyWebSocketClient

class SpringJettyWebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-spring-jetty-client") {

    override fun provideClient(): WebSocketClient = JettyWebSocketClient().apply { start() }.asKrossbowWebSocketClient()
}
