package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*

private val Platform.statusCodeSupport: StatusCodeSupport
    get() = when (this) {
        Platform.Js.Browser,
        Platform.WasmJs.Browser -> StatusCodeSupport.None // browser cannot support status codes for security reasons
        Platform.Js.NodeJs,
        Platform.WasmJs.NodeJs,
        Platform.WasmWasi.NodeJs -> StatusCodeSupport.All // JS node: supports status codes since Kotlin 2.0
        Platform.Windows -> StatusCodeSupport.None // WinHttp: error is too generic and doesn't differ per status code
        is Platform.Apple,
        Platform.Jvm,
        Platform.Linux -> StatusCodeSupport.All // CIO engine (classpath order), which reports status codes
    }

// This test is somewhat redundant with the tests on specific platforms, but it ensures that we don't forget to test
// new Ktor-supported platforms when they are added to the Krossbow projects.
// Also, it covers cases of dynamically-selected implementations.
class KtorMppWebSocketClientTest : WebSocketClientTestSuite(
    statusCodeSupport = currentPlatform().statusCodeSupport,
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
