package org.hildan.krossbow.websocket.js

import IsomorphicJsWebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JsWebSocketClientConnectTest {

    @Test
    fun testConnectFailure() = runSuspendingTest {
        val client = IsomorphicJsWebSocketClient

        assertFailsWith(WebSocketConnectionException::class) {
            client.connect("ws://garbage")
        }
    }
}
