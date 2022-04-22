package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.websocket.*
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class KtorOkHttpWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = KtorWebSocketClient(HttpClient(OkHttp) { install(WebSockets) })
}
