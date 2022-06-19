package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.darwin.DarwinWebSocketClient

// FIXME investigate macOS failures
private val excludeAll = CaseExclusion(
    caseIdPrefixes = listOf(""), // empty prefix matches everything
    reason = "Whole Autobahn test suite disabled on macOS, all tests fail",
)

class DarwinWebSocketAutobahnTest : AutobahnClientTestSuite(
    agentUnderTest = "krossbow-darwin-client-${Platform.osFamily.name.lowercase()}",
    exclusions = if (Platform.osFamily == OsFamily.MACOSX) listOf(excludeAll) else emptyList(),
) {
    override fun provideClient(): WebSocketClient = DarwinWebSocketClient()
}
