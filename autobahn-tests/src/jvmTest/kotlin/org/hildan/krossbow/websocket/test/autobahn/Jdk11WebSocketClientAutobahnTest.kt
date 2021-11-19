package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient

class Jdk11WebSocketClientAutobahnTest : AutobahnClientTestSuite(
    agentUnderTest = "krossbow-jdk11-client",
    exclusions = listOf(
        CaseExclusion(
            caseIdPrefixes = listOf("2.10", "2.11"),
            reason = "Autobahn ping-pong tests are stricter than the spec. The JDK11 client sometimes just sends the " +
                "last PONG, which is conform to the spec and should be accepted by the test.",
        ),
    ),
) {
    override fun provideClient() = Jdk11WebSocketClient()
}
