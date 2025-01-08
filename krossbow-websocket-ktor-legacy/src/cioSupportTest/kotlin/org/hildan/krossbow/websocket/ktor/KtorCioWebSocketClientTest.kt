package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*

class KtorCioWebSocketClientTest : WebSocketClientTestSuite() {

    override fun provideClient(): WebSocketClient = KtorWebSocketClient(
        HttpClient(CIO) {
            // The CIO engine seems to follow 301 redirects by default, but our test server doesn't provide a Location
            // header with the URL to redirect to, so the client retries the same URL indefinitely.
            // To avoid a SendCountExceedException in status code tests, we disable redirect-following explicitly here.
            followRedirects = false

            install(WebSockets)
        },
    )
}
