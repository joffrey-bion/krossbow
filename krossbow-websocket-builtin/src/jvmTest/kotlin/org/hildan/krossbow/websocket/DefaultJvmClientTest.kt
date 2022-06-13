package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient
import kotlin.test.Test
import kotlin.test.assertTrue

class DefaultJvmClientTest {

    @Test
    fun defaultClientTest() {
        assertTrue(WebSocketClient.default() is Jdk11WebSocketClient)
    }
}
