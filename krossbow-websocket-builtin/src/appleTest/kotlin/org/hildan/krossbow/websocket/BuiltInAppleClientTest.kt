package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.builtin.*
import org.hildan.krossbow.websocket.darwin.*
import kotlin.test.*

class BuiltInAppleClientTest {

    @Test
    fun defaultClientTest() {
        assertTrue(WebSocketClient.builtIn() is DarwinWebSocketClient)
    }
}
