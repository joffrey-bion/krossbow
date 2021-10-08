package org.hildan.krossbow.websocket.okhttp

import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class OkHttpWebSocketClientConnectTest {

    @Test
    fun testConnectFailure() = runSuspendingTest {
        val client = OkHttpWebSocketClient()

        assertFailsWith<WebSocketConnectionException> {
            client.connect("ws://garbage")
        }
    }
}
