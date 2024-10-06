package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.builtin.*
import org.hildan.krossbow.websocket.js.*
import kotlin.test.*

class BuiltInJsClientTest {

    @Test
    fun defaultClientTest() {
        assertTrue(WebSocketClient.builtIn() is BrowserWebSocketClient)
    }
}
