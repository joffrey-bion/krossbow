package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.websocket.*
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

class KtorLegacyOkHttpClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-legacy-okhttp-client") {

    override fun provideClient() = KtorWebSocketClient(HttpClient(OkHttp) { install(WebSockets) })
}
