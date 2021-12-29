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
    fun testEchoText() = runSuspendingTestWithEchoServer { server ->
        val session = wsClient.connect(server.localUrl)

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
    fun testEchoBinary() = runSuspendingTestWithEchoServer { server ->
        val session = wsClient.connect(server.localUrl)

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
    fun testClose() = runSuspendingTestWithEchoServer { server ->
        val session = wsClient.connect(server.localUrl)

        server.close()

        session.expectCloseFrame("after connect")
        session.expectNoMoreFrames("after CLOSE frame following connect")
    }
}

private fun runSuspendingTestWithEchoServer(block: suspend (server: TestServer) -> Unit) {
    runSuspendingTest {
        runAlongEchoWSServer { server ->
            block(server)
        }
    }
}

private val TestServer.localUrl: String
    get() = "ws://localhost:$port"
