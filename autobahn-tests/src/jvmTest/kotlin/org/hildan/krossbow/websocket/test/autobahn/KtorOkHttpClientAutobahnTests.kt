package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.websocket.*
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

class KtorOkHttpClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-okhttp-client") {

    override fun provideClient() = KtorWebSocketClient(HttpClient(OkHttp) { install(WebSockets) })
}
