package org.hildan.krossbow.websocket.jdk

import org.hildan.krossbow.websocket.test.autobahn.AutobahnClientTestSuite

class Jdk11WebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-jdk11-client") {

    override fun provideClient() = Jdk11WebSocketClient()
}
