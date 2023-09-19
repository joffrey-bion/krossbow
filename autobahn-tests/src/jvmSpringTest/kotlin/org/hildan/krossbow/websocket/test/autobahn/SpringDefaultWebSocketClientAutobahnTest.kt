package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.spring.asKrossbowWebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import kotlin.test.Ignore

// FIXME: investigate RejectedExecutionException on plain echo tests (thread pool is shutting down but the websocket
//  should not be closed at that time). Only happens on the CI
@Ignore
class SpringDefaultWebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-spring-default-client") {

    override fun provideClient(): WebSocketClient = StandardWebSocketClient().asKrossbowWebSocketClient()
}
