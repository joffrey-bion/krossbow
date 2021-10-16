package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.features.websocket.*
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

class KtorJavaClientAutobahnTests : AutobahnClientTestSuite(
    agentUnderTest = "krossbow-ktor-java-client",
    exclusions = listOf(
        CaseExclusion(
            caseIdPrefixes = listOf("2.", "5.6", "5.7", "5.8"),
            reason = "Ktor sends double pongs with Java engine, which breaks autobahn ping-pong tests",
        ),
        // FIXME this exclusions should be investigated and fixed
        CaseExclusion(
            caseIdPrefixes = listOf(
                "3.1", "3.5", "3.6", "3.7", "4.1.1", "4.1.2", "4.2.1", "4.2.2", "5.1", "5.2", "5.9",
            ),
            reason = "This test times out every time (to be investigated)",
        ),
    ),
) {

    override fun provideClient() = KtorWebSocketClient(HttpClient(Java) { install(WebSockets) })
}
