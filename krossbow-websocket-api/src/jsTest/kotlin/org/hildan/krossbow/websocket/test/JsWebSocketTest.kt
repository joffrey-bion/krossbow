package org.hildan.krossbow.websocket.test

import org.hildan.krossbow.engines.mpp.js.BrowserWebSocketClient
import org.hildan.krossbow.engines.mpp.js.SockJSWebSocketClient
import kotlin.test.Ignore
import kotlin.test.Test

class JsWebSocketTest {

    // TODO provide WebSocket for node somehow (using isomorphic-ws package?)
    @Ignore
    @Test
    fun test_browser() {
        testKaazingEchoWs(BrowserWebSocketClient, "ws")
    }

    @Test
    fun test_sockjs() {
        testKaazingEchoWs(SockJSWebSocketClient, "http")
    }
}
