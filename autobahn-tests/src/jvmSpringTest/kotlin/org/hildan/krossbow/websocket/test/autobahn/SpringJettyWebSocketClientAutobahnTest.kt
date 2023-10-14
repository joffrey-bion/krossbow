@file:Suppress("removal")

package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.spring.asKrossbowWebSocketClient

class SpringJettyWebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-spring-jetty-client") {

    @Suppress("DEPRECATION") // this test will be removed when we remove support for JettyWebSocketClient
    override fun provideClient(): WebSocketClient =
        org.springframework.web.socket.client.jetty.JettyWebSocketClient().apply { start() }.asKrossbowWebSocketClient()
}
