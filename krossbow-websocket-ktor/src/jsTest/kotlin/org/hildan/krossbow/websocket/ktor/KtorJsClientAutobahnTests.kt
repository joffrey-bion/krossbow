package org.hildan.krossbow.websocket.ktor

import org.hildan.krossbow.websocket.test.autobahn.AutobahnClientTestSuite
import kotlin.test.Ignore

@Ignore // like other JS autobahn tests, cannot run at the moment on macOS runner
class KtorJsClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-js-client") {

    override fun provideClient() = KtorWebSocketClient()
}
