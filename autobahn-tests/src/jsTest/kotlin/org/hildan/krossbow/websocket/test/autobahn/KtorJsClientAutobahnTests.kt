package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import org.hildan.krossbow.websocket.test.environment
import kotlin.test.Ignore

// FIXME part of these tests fail due to getCaseStatus abrupt connection close
// FIXME part of these tests fail with IllegalStateException: Already resumed, but proposed with update CompletedExceptionally
//   The underlying error is part of the test, for instance: "nvalid WebSocket frame: RSV1 must be clear"
//   But for some reason Ktor tries to resume a continuation twice?
@Ignore
class KtorJsClientAutobahnTests : AutobahnClientTestSuite("krossbow-ktor-js-client-${environment()}") {

    override fun provideClient() = KtorWebSocketClient()
}
