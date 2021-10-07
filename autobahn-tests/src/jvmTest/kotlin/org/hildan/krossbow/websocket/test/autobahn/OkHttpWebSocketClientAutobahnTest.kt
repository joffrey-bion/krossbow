package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class OkHttpWebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-okhttp-client") {

    override fun provideClient() = OkHttpWebSocketClient()
}
