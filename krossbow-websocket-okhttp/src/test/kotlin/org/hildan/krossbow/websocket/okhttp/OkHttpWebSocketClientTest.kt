package org.hildan.krossbow.websocket.okhttp

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class OkHttpWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = OkHttpWebSocketClient()
}
