package org.hildan.krossbow.websocket.ktor

import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.ktor.test.runSuspendingTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorWebSocketTest {

    @Ignore // remove dependency on the internet
    @Test
    fun test_krossbow_ktor() = runSuspendingTest {
        val client = KtorWebSocketClient()
        val connection: WebSocketConnection = client.connect("ws://echo.websocket.org")
        connection.sendText("hello")
        val msg = connection.incomingFrames.receive()
        assertEquals("hello", (msg as WebSocketFrame.Text).text)
        connection.close()
    }
}
