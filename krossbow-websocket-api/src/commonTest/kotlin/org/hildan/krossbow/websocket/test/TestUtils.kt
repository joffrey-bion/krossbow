package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketFrame
import kotlin.test.assertEquals
import kotlin.test.assertTrue

expect fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit)

fun testKaazingEchoWs(websocketClient: WebSocketClient, protocol: String) = runSuspendingTest {
    val session = websocketClient.connect("$protocol://demos.kaazing.com/echo")

    session.sendText("hello")
    val helloResponse = session.incomingFrames.receive()
    assertTrue(helloResponse is WebSocketFrame.Text)
    assertEquals("hello", helloResponse.text)

    val fortyTwos = ByteArray(3) { 42 }
    session.sendBinary(fortyTwos)
    val fortyTwosResponse = session.incomingFrames.receive()
    assertTrue(fortyTwosResponse is WebSocketFrame.Binary)
    assertEquals(fortyTwos.toList(), fortyTwosResponse.bytes.toList())

    session.close()
}
