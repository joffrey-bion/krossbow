package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.test.*

// WinHttp: error is too generic and doesn't differ per status code
// JS node: error is too generic and doesn't differ per status code (ECONNREFUSED, unlike 'ws')
// JS browser: cannot support status codes for security reasons
// Other: currently the other platforms use the CIO engine because of classpath order, and CIO supports status codes
private val Platform.supportsStatusCodes: Boolean
    get() = this !is Platform.Windows && this !is Platform.Js

class KtorMppWebSocketClientTest : WebSocketClientTestSuite(
    supportsStatusCodes = currentPlatform().supportsStatusCodes,
    supportsCustomHeaders = currentPlatform() !is Platform.Js.Browser,
) {
    override fun provideClient(): WebSocketClient = KtorWebSocketClient(
        HttpClient {
            // The CIO engine seems to follow 301 redirects by default, but our test server doesn't provide a URL to
            // it retries the same URL indefinitely and reach a SendCountExceedException.
            // To avoid this failure in status code tests, we disable redirect-following explicitly here.
            followRedirects = false

            install(WebSockets)
        },
    )
}
