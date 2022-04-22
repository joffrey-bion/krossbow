package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.websocket.*
import org.hildan.krossbow.websocket.ktor.KtorLegacyWebSocketClient

class KtorLegacyOkHttpClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-legacy-okhttp-client") {

    override fun provideClient() = KtorLegacyWebSocketClient(HttpClient(OkHttp) { install(WebSockets) })
}
