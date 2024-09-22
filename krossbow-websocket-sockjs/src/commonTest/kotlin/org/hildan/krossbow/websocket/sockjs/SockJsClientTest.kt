package org.hildan.krossbow.websocket.sockjs

import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*

class SockJSClientTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = SockJSClient()
}
