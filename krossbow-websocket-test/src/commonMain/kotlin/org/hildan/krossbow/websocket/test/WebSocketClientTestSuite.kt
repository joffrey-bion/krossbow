package org.hildan.krossbow.websocket.test

import org.hildan.krossbow.websocket.*
import kotlin.test.*

abstract class WebSocketClientTestSuite(val supportsStatusCodes: Boolean = true) {

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
    fun testConnectFailure_correctStatusCodeInException() = runSuspendingTest {
        runAlongHttpServer { baseUrl ->
            assertCorrectStatusReported(baseUrl, 200) // not good for WS, should be 101
            assertCorrectStatusReported(baseUrl, 301)
            assertCorrectStatusReported(baseUrl, 302)
            assertCorrectStatusReported(baseUrl, 401)
            assertCorrectStatusReported(baseUrl, 403)
            assertCorrectStatusReported(baseUrl, 404)
            assertCorrectStatusReported(baseUrl, 500)
            assertCorrectStatusReported(baseUrl, 503)
        }
    }

    private suspend fun assertCorrectStatusReported(baseUrl: String, statusCodeToTest: Int) {
        // Using the non-reified version because of the suspend inline function bug on JS platform
        // https://youtrack.jetbrains.com/issue/KT-37645
        val ex = assertFailsWith(WebSocketConnectionException::class) {
            wsClient.connect("$baseUrl/failHandshakeWithStatusCode/$statusCodeToTest")
        }
        if (supportsStatusCodes) {
            assertEquals(statusCodeToTest, ex.httpStatusCode)
        } else {
            assertNull(ex.httpStatusCode, "${wsClient::class} is not expected to support status codes")
        }
    }

    @IgnoreOnNative
    @IgnoreOnJS
    @Test
    fun testHandshakeHeaders() = runSuspendingTestWithEchoServer { server ->
        val session = wsClient.connect(
            url = server.localUrl,
            headers = mapOf("My-Header-1" to "my-value-1", "My-Header-2" to "my-value-2"),
        )
        val header = session.expectTextFrame("header info frame")
        assertEquals("custom-headers:My-Header-1=my-value-1, My-Header-2=my-value-2", header.text)
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
