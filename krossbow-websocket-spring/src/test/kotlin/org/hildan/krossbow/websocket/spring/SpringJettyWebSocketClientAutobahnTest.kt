package org.hildan.krossbow.websocket.spring

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.autobahn.AutobahnClientTestSuite

class SpringJettyWebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-spring-jetty-client") {

    override fun provideClient(): WebSocketClient = SpringJettyWebSocketClient
}
