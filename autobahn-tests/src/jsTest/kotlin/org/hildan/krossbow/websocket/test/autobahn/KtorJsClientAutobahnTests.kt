package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import org.hildan.krossbow.websocket.test.environment
import kotlin.test.Ignore

@Ignore // like other JS autobahn tests, cannot run at the moment on macOS runner
class KtorJsClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-js-client-${environment()}") {

    override fun provideClient() = KtorWebSocketClient()
}
