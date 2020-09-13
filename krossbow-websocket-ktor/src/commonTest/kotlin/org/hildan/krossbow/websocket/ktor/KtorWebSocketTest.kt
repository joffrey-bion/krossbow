package org.hildan.krossbow.websocket.ktor

import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketSession
import org.hildan.krossbow.websocket.ktor.test.runSuspendingTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorWebSocketTest {

    @Ignore // remove dependency on the internet
    @Test
    fun test_krossbow_ktor() = runSuspendingTest {
        val client = KtorWebSocketClient()
        val session: WebSocketSession = client.connect("ws://echo.websocket.org")
        session.sendText("hello")
        val msg = session.incomingFrames.receive()
        assertEquals("hello", (msg as WebSocketFrame.Text).text)
        session.close()
    }
}
