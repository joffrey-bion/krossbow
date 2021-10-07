package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.test.Ignore

@Ignore // like other JS autobahn tests, cannot run at the moment on macOS runner
class KtorJsClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-js-client") {

    override fun provideClient() = KtorWebSocketClient()
}
