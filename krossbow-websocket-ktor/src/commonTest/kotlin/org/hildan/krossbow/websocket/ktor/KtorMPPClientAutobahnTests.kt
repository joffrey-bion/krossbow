package org.hildan.krossbow.websocket.ktor

import org.hildan.krossbow.websocket.test.autobahn.AutobahnClientTestSuite
import kotlin.test.Ignore

@Ignore // ignored because JS autobahn tests cannot run at the moment on macOS runner
class KtorMPPClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-default-mpp-client") {

    override fun provideClient() = KtorWebSocketClient()
}
