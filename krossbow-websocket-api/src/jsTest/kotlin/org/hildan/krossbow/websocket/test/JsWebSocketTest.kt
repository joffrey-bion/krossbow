package org.hildan.krossbow.websocket.test

import org.hildan.krossbow.websocket.js.BrowserWebSocketClient
import kotlin.test.Ignore
import kotlin.test.Test

class JsWebSocketTest {

    // TODO provide WebSocket for node somehow (using isomorphic-ws package?)
    @Ignore
    @Test
    fun test_browser() {
        testKaazingEchoWs(BrowserWebSocketClient, "ws")
    }
}
