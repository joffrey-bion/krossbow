package org.hildan.krossbow.websocket.test

import org.hildan.krossbow.websocket.spring.SpringDefaultWebSocketClient
import org.hildan.krossbow.websocket.spring.SpringSockJSWebSocketClient
import kotlin.test.Test

class SpringWebSocketClientAdapterTest {

    @Test
    fun test_spring_default() {
        testKaazingEchoWs(SpringDefaultWebSocketClient, "ws")
    }

    @Test
    fun test_spring_sockjs() {
        testKaazingEchoWs(SpringSockJSWebSocketClient, "http")
    }
}
