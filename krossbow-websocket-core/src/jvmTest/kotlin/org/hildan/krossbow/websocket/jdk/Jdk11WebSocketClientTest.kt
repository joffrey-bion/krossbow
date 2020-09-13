package org.hildan.krossbow.websocket.jdk

import org.hildan.krossbow.websocket.test.EchoWebSocketServer
import org.hildan.krossbow.websocket.test.testEchoWs
import kotlin.test.Test

class Jdk11WebSocketClientTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun test() {
        val server = EchoWebSocketServer()
        val port = server.startAndAwaitPort()
        testEchoWs(Jdk11WebSocketClient(), "ws://localhost:$port")
        server.stop()
    }
}
