package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.builtin.builtIn
import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient
import kotlin.test.Test
import kotlin.test.assertTrue

class BuiltInJvmClientTest {

    @Test
    fun defaultClientTest() {
        assertTrue(WebSocketClient.builtIn() is Jdk11WebSocketClient)
    }
}
