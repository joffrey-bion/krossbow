package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import org.hildan.krossbow.websocket.test.environment
import kotlin.test.Ignore

// FIXME these tests don't work because Ktor 2 is incorrectly present at runtime (due to how JS modules are organized)
//   See https://youtrack.jetbrains.com/issue/KT-31504

// When the test worked, they did have issues too:
// FIXME part of these tests fail due to getCaseStatus abrupt connection close
// FIXME part of these tests fail with IllegalStateException: Already resumed, but proposed with update CompletedExceptionally
//   The underlying error is part of the test, for instance: "invalid WebSocket frame: RSV1 must be clear"
//   But for some reason Ktor tries to resume a continuation twice?
@Ignore
class KtorLegacyJsClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-legacy-js-client-${environment()}") {

    override fun provideClient() = KtorWebSocketClient()
}
