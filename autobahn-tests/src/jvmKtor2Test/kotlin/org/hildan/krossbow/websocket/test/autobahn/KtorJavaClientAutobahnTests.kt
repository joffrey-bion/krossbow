package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.websocket.*
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
            reason = "This test times out every time even locally (to be investigated)",
        ),
        // FIXME this exclusions should be investigated and fixed
        CaseExclusion(
            caseIdPrefixes = listOf("3.2", "3.3", "3.4", "4.1.3", "4.1.4", "4.1.5", "4.2.3", "4.2.4", "4.2.5"),
            reason = "This test times out every time on CI (to be investigated)",
        ),
    ),
) {

    override fun provideClient() = KtorWebSocketClient(HttpClient(Java) { install(WebSockets) })
}
