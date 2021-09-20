package org.hildan.krossbow.websocket.jdk

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.default
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Jdk11WebSocketClientConnectTest {

    @Test
    fun testConnectFailure() = runSuspendingTest {
        val client = WebSocketClient.default()

        assertFailsWith<WebSocketConnectionException> {
            client.connect("ws://garbage")
        }
    }
}
