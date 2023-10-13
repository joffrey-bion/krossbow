package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.experimental.*

@OptIn(ExperimentalNativeApi::class)
class KtorCioWebSocketAutobahnTest : AutobahnClientTestSuite(
    agentUnderTest = "krossbow-ktor-cio-client-${Platform.osFamily.name.lowercase()}",
    exclusions = listOf(
        CaseExclusion(
            caseIdPrefixes = listOf("2.5"),
            reason = "CIO is more lenient than the spec, and accept pings > 125 bytes",
        ),
        CaseExclusion(
            caseIdPrefixes = listOf("3.1", "3.2", "3.3", "3.4", "3.5", "3.6"),
            reason = "CIO is more lenient than the spec, and doesn't fail on non-negotiated RSV != 0",
        ),
        CaseExclusion(
            caseIdPrefixes = listOf("5.1", "5.2"),
            reason = "CIO is more lenient than the spec, and doesn't fail on fragmented pings/pongs",
        ),
        // FIXME this is not critical but should be investigated
        CaseExclusion(
            caseIdPrefixes = listOf("4."),
            reason = "CIO doesn't seem to fail on reserved op codes, thus timing out waiting for more frames",
        ),
        // FIXME this is not critical but should be investigated
        CaseExclusion(
            caseIdPrefixes = listOf("5.6", "5.7", "5.8"),
            reason = "non-deterministic ordering in case of mixed pings with fragmented messages",
        ),
        // FIXME this is not critical but should be investigated
        CaseExclusion(
            caseIdPrefixes = listOf("5.9"),
            reason = "CIO doesn't seem to fail the connection in a way that's visible to the client, so the test " +
                "hangs waiting for more frames to echo",
        ),
    ),
) {
    override fun provideClient(): WebSocketClient = KtorWebSocketClient(HttpClient(CIO) { install(WebSockets) })
}
