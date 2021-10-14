package org.hildan.krossbow.websocket.test

import org.hildan.krossbow.websocket.*
import kotlin.test.*

abstract class WebSocketClientTestSuite {

    abstract fun provideClient(): WebSocketClient

    private lateinit var wsClient: WebSocketClient

    @BeforeTest
    fun setupClient() {
        wsClient = provideClient()
    }

    @Test
    fun testConnectFailure() = runSuspendingTest {
        // Using the non-reified version because of the suspend inline function bug on JS platform
        // https://youtrack.jetbrains.com/issue/KT-37645
        assertFailsWith(WebSocketConnectionException::class) {
            wsClient.connect("ws://garbage")
        }
    }

    @IgnoreOnNative
    @IgnoreOnJS
    @Test
    fun testEchoText() = runSuspendingTestWithEchoServer { url ->
        val session = wsClient.connect(url)

        session.sendText("hello")
        val helloResponse = session.expectTextFrame("hello frame")
        assertEquals("hello", helloResponse.text)

        session.close()

        session.expectCloseFrame("after echo text")
        session.expectNoMoreFrames("after echo text CLOSE frame")
    }

    @IgnoreOnNative
    @IgnoreOnJS
    @Test
    fun testEchoBinary() = runSuspendingTestWithEchoServer { url ->
        val session = wsClient.connect(url)

        val fortyTwos = ByteArray(3) { 42 }
        session.sendBinary(fortyTwos)
        val fortyTwosResponse = session.expectBinaryFrame("3 binary 42s")
        assertEquals(fortyTwos.toList(), fortyTwosResponse.bytes.toList())

        session.close()

        session.expectCloseFrame("after echo binary")
        session.expectNoMoreFrames("after echo binary CLOSE frame")
    }

    @IgnoreOnNative
    @IgnoreOnJS
    @Test
    fun testClose() = runSuspendingTestWithEchoServer(onOpenActions = {
        close()
    }) { url ->
        val session = wsClient.connect(url)

        session.expectCloseFrame("after connect")
        session.expectNoMoreFrames("after CLOSE frame following connect")
    }
}

private fun runSuspendingTestWithEchoServer(
    onOpenActions: ActionsBuilder.() -> Unit = {},
    block: suspend (url: String) -> Unit,
) {
    runSuspendingTest {
        runAlongEchoWSServer(onOpenActions) { port ->
            block("ws://localhost:$port")
        }
    }
}
