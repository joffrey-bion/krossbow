package org.hildan.krossbow.websocket.test

import kotlinx.io.bytestring.*
import org.hildan.krossbow.websocket.*
import kotlin.test.*

abstract class WebSocketClientTestSuite(
    val supportsStatusCodes: Boolean = true,
    val supportsCustomHeaders: Boolean = true,
) {
    abstract fun provideClient(): WebSocketClient

    private lateinit var wsClient: WebSocketClient

    companion object {
        private val testServerConfig: TestServerConfig = getTestServerConfig()
    }

    @BeforeTest
    fun setupClient() {
        wsClient = provideClient()
    }

    @Test
    fun testConnectFailure_failsOnUnresolvedHost() = runSuspendingTest {
        // Using the non-reified version because of the suspend inline function bug on JS platform
        // https://youtrack.jetbrains.com/issue/KT-37645
        val e = assertFailsWith(WebSocketConnectionException::class) {
            wsClient.connect("ws://garbage")
        }
        val message = e.message
        assertNotNull(message, "Connection exception should have a message")
        assertContains(message, "Couldn't connect to web socket at ws://garbage")
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_2xx() = runSuspendingTest {
        assertCorrectStatusesReported(200..208) // 2xx is not good for WS, should be 101
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_3xx() = runSuspendingTest {
        assertCorrectStatusesReported(300..308)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_4xx() = runSuspendingTest {
        assertCorrectStatusesReported(400..418)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_5xx() = runSuspendingTest {
        assertCorrectStatusesReported(500..508)
    }

    private suspend fun assertCorrectStatusesReported(statusCodesToTest: IntRange) =
        statusCodesToTest.forEach { assertCorrectStatusReported(it) }

    private suspend fun assertCorrectStatusReported(statusCodeToTest: Int) {
        // Using the non-reified version because of the suspend inline function bug on JS platform
        // https://youtrack.jetbrains.com/issue/KT-37645
        val ex = assertFailsWith(WebSocketConnectionException::class) {
            wsClient.connect("${testServerConfig.wsUrlWithHttpPort}/failHandshakeWithStatusCode/$statusCodeToTest")
        }
        if (supportsStatusCodes) {
            assertEquals(
                statusCodeToTest,
                ex.httpStatusCode,
                "missing status code $statusCodeToTest in connection exception $ex, cause:\n${ex.cause?.stackTraceToString()}",
            )
        } else {
            assertNull(ex.httpStatusCode, "${wsClient::class} is not expected to support status codes")
        }
    }

    @Test
    fun testHandshakeHeaders() = runSuspendingTest {
        val connectWithTestHeaders = suspend {
            wsClient.connect(
                url = testServerConfig.wsUrl,
                headers = mapOf("My-Header-1" to "my-value-1", "My-Header-2" to "my-value-2"),
            )
        }
        if (supportsCustomHeaders) {
            val session = connectWithTestHeaders()
            val header = session.expectTextFrame("header info frame")
            assertEquals("custom-headers:My-Header-1=my-value-1, My-Header-2=my-value-2", header.text)
        } else {
            assertFailsWith<IllegalArgumentException> { connectWithTestHeaders() }
        }
    }

    @Test
    fun testEchoText() = runSuspendingTest {
        val session = wsClient.connect(testServerConfig.wsUrl)

        session.sendText("hello")
        val helloResponse = session.expectTextFrame("hello frame")
        assertEquals("hello", helloResponse.text)

        session.close()

        session.expectCloseFrame("after echo text")
        session.expectNoMoreFrames("after echo text CLOSE frame")
    }

    @Test
    fun testEchoBinary() = runSuspendingTest {
        val session = wsClient.connect(testServerConfig.wsUrl)

        val fortyTwos = ByteString(42, 42, 42)
        session.sendBinary(fortyTwos)
        val fortyTwosResponse = session.expectBinaryFrame("3 binary 42s")
        assertEquals(fortyTwos, fortyTwosResponse.bytes)

        session.close()

        session.expectCloseFrame("after echo binary")
        session.expectNoMoreFrames("after echo binary CLOSE frame")
    }
}
