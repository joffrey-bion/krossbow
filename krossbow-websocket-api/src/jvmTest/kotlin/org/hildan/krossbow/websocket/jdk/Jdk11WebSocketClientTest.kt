package org.hildan.krossbow.websocket.jdk

import org.hildan.krossbow.websocket.test.testKaazingEchoWs
import kotlin.test.Test

class Jdk11WebSocketClientTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun test() {
        testKaazingEchoWs(Jdk11WebSocketClient(), "ws")
    }
}
