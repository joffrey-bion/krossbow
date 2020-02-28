package org.hildan.krossbow.websocket.test

import kotlin.test.Test

class JsWebSocketTest {

    // TODO provide WebSocket for node somehow (using isomorphic-ws package?)
    @Test
    fun test_browser() {
        // testKaazingEchoWs(BrowserWebSocketClient, "ws")
    }

    // TODO check this test out, this may require a SockJS server
    @Test
    fun test_sockjs() {
        // testKaazingEchoWs(SockJSWebSocketClient, "http")
    }
}
