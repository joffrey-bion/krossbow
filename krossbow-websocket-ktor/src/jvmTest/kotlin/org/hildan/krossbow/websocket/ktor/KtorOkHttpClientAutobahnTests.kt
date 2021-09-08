package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.websocket.*
import org.hildan.krossbow.websocket.test.autobahn.AutobahnClientTestSuite
import kotlin.test.Ignore

@Ignore // ignored because of problems with CLOSE frame (either not there, or too many when we use onComplete)
class KtorOkHttpClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-okhttp-client") {

    override fun provideClient() = KtorWebSocketClient(HttpClient(OkHttp) { install(WebSockets) })
}
