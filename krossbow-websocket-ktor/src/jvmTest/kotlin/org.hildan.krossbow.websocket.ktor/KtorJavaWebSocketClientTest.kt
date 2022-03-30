package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.WebSocketClientTestSuite

class KtorJavaWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = KtorWebSocketClient(HttpClient(Java) { install(WebSockets) })
}
