package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.delay
import org.hildan.krossbow.engines.mpp.js.BrowserWebSocketClient
import org.hildan.krossbow.engines.mpp.js.SockJSWebSocketClient
import org.w3c.dom.WebSocket
import kotlin.test.Test

class JsWebSocketTest {

    @Test
    fun test_pure() = runSuspendingTest {
        val ws = WebSocket("ws://demos.kaazing.com/echo")
        ws.onopen = { println("OPENED!") }
        delay(500)
        ws.close()
    }

    @Test
    fun test_browser() {
        testKaazingEchoWs(BrowserWebSocketClient, "ws")
    }

    @Test
    fun test_sockjs() {
        testKaazingEchoWs(SockJSWebSocketClient, "http")
    }
}
