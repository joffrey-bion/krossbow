package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*

// WinHttp: error is too generic and doesn't differ per status code
// JS browser: cannot support status codes for security reasons
// JS node: supports status codes since Kotlin 2.0
// Other: currently the other platforms use the CIO engine because of classpath order, and CIO supports status codes
private val Platform.supportsStatusCodes: Boolean
    get() = this !is Platform.Windows && this !is Platform.Js.Browser

// This test is somewhat redundant with the tests on specific platforms, but it ensures that we don't forget to test
// new Ktor-supported platforms when they are added to the Krossbow projects.
// Also, it covers cases of dynamically-selected implementations.
class KtorMppWebSocketClientTest : WebSocketClientTestSuite(
    supportsStatusCodes = currentPlatform().supportsStatusCodes,
    // Just to be sure we don't attempt to test this with the Java or JS engines
    // See https://youtrack.jetbrains.com/issue/KTOR-6970
    shouldTestNegotiatedSubprotocol = false,
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
