package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketFrame
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class WebSocketClientTestSuite {

    abstract fun provideClient(): WebSocketClient

    private lateinit var wsClient: WebSocketClient

    @BeforeTest
    fun setupClient() {
        wsClient = provideClient()
    }

    @Test
    fun testWithEchoServer() = runSuspendingTest {
        runAlongEchoWSServer { port ->
            testEchoWs(wsClient, "ws://localhost:$port")
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun testEchoWs(websocketClient: WebSocketClient, url: String) {
    val session = websocketClient.connect(url)

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

    val closeFrame = session.incomingFrames.receive()
    assertTrue(closeFrame is WebSocketFrame.Close, "Last frame should be a close frame")
    assertEquals(WebSocketCloseCodes.NORMAL_CLOSURE, closeFrame.code)

    delay(20) // somehow isClosedForReceive needs some time to become true
    assertTrue(session.incomingFrames.isClosedForReceive, "The incoming frames channel should be closed")
}
