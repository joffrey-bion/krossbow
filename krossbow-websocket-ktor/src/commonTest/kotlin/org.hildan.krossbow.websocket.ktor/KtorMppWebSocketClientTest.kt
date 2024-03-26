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

// This test is somewhat redundant with the tests on specific platforms, but it ensures that we don't forget to test
// new Ktor-supported platforms when they are added to the Krossbow projects.
// Also, it covers cases of dynamically-selected implementations.
class KtorMppWebSocketClientTest : WebSocketClientTestSuite(
    supportsStatusCodes = currentPlatform().supportsStatusCodes,
) {
    override fun provideClient(): WebSocketClient = KtorWebSocketClient(
        HttpClient {
            // The CIO engine seems to follow 301 redirects by default, but our test server doesn't provide a Location
            // header with the URL to redirect to, so the client retries the same URL indefinitely.
            // To avoid a SendCountExceedException in status code tests, we disable redirect-following explicitly here.
            followRedirects = false

            install(WebSockets)
        },
    )
}
