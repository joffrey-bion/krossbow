package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.features.websocket.*
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.test.Ignore

@Ignore // ignored because double ping-pong management fails Autobahn tests 2.x
class KtorJavaClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-java-client") {

    override fun provideClient() = KtorWebSocketClient(HttpClient(Java) { install(WebSockets) })
}
