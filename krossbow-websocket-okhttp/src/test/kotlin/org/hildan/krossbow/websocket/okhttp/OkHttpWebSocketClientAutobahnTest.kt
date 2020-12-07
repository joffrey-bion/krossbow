package org.hildan.krossbow.websocket.okhttp

import org.hildan.krossbow.websocket.test.autobahn.AutobahnClientTestSuite
import kotlin.test.Ignore

// FIXME somehow the OkHttp tests always hang and timeout
@Ignore
class OkHttpWebSocketClientAutobahnTest : AutobahnClientTestSuite("krossbow-okhttp-client") {

    override fun provideClient() = OkHttpWebSocketClient()
}
