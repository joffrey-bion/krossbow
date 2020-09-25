package org.hildan.krossbow.websocket.jdk

import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class Jdk11WebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient() = Jdk11WebSocketClient()
}
