package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.features.websocket.*
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.test.Ignore

@Ignore // FIXME weird timeouts on lots of cases
class KtorJavaClientAutobahnTests : AutobahnClientTestSuite(
    agentUnderTest = "krossbow-ktor-java-client",
    exclusions = listOf(
        CaseExclusion(
            caseIdPrefixes = listOf("2.", "5.6", "5.7", "5.8"),
            reason = "Ktor sends double pongs with Java engine, which breaks autobahn ping-pong tests",
        ),
    ),
) {

    override fun provideClient() = KtorWebSocketClient(HttpClient(Java) { install(WebSockets) })
}
