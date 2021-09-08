package org.hildan.krossbow.websocket.okhttp

import org.hildan.krossbow.websocket.test.autobahn.AutobahnClientTestSuite

class OkHttpWebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-okhttp-client") {

    override fun provideClient() = OkHttpWebSocketClient()
}
