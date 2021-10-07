package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient

class Jdk11WebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-jdk11-client") {

    override fun provideClient() = Jdk11WebSocketClient()
}
