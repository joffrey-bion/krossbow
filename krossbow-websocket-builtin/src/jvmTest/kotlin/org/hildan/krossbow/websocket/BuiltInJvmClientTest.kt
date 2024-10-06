package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.builtin.*
import org.hildan.krossbow.websocket.jdk.*
import kotlin.test.*

class BuiltInJvmClientTest {

    @Test
    fun defaultClientTest() {
        assertTrue(WebSocketClient.builtIn() is Jdk11WebSocketClient)
    }
}
