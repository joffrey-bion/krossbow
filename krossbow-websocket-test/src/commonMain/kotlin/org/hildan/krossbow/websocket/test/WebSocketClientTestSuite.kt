package org.hildan.krossbow.websocket.test

import kotlinx.io.bytestring.*
import org.hildan.krossbow.websocket.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

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
    fun testConnectFailure_correctStatusCodeInException_200() = runSuspendingTest {
        assertCorrectStatusReported(200) // 200 is not good for WS, should be 101
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_201_to_208() = runSuspendingTest {
        assertCorrectStatusesReported(201..208) // 2xx is not good for WS, should be 101
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_301() = runSuspendingTest {
        assertCorrectStatusReported(301)
    }

    // This range is broken down to avoid exceeding the test timeout and also helps to see issues with specific codes
    @Test
    fun testConnectFailure_correctStatusCodeInException_302_to_305() = runSuspendingTest(timeout = 30.seconds) {
        assertCorrectStatusesReported(302..305)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_306_to_308() = runSuspendingTest(timeout = 30.seconds) {
        assertCorrectStatusesReported(306..308)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_400() = runSuspendingTest {
        assertCorrectStatusReported(400)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_401() = runSuspendingTest {
        assertCorrectStatusReported(401)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_402() = runSuspendingTest {
        assertCorrectStatusReported(402)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_403() = runSuspendingTest {
        assertCorrectStatusReported(403)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_404() = runSuspendingTest {
        assertCorrectStatusReported(404)
    }

    // This range is broken down to avoid exceeding the test timeout and also helps to see issues with specific codes
    @Test
    fun testConnectFailure_correctStatusCodeInException_405_to_408() = runSuspendingTest(timeout = 30.seconds) {
        assertCorrectStatusesReported(405..408)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_409_to_412() = runSuspendingTest(timeout = 30.seconds) {
        assertCorrectStatusesReported(409..412)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_413_to_415() = runSuspendingTest(timeout = 30.seconds) {
        assertCorrectStatusesReported(413..415)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_416_to_418() = runSuspendingTest(timeout = 30.seconds) {
        assertCorrectStatusesReported(416..418)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_500() = runSuspendingTest {
        assertCorrectStatusReported(500)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_501() = runSuspendingTest {
        assertCorrectStatusReported(501)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_502() = runSuspendingTest {
        assertCorrectStatusReported(502)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_503() = runSuspendingTest {
        assertCorrectStatusReported(503)
    }

    // This range is broken down to avoid exceeding the test timeout and also helps to see issues with specific codes
    @Test
    fun testConnectFailure_correctStatusCodeInException_504_to_506() = runSuspendingTest(timeout = 30.seconds) {
        assertCorrectStatusesReported(504..506)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_507_to_508() = runSuspendingTest(timeout = 30.seconds) {
        assertCorrectStatusesReported(507..508)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_510() = runSuspendingTest {
        assertCorrectStatusReported(510)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_511() = runSuspendingTest {
        assertCorrectStatusReported(511)
    }

    private suspend fun assertCorrectStatusesReported(statusCodesToTest: Iterable<Int>) =
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
                url = "${testServerConfig.wsUrl}/echoHeaders",
                headers = mapOf("My-Header-1" to "my-value-1", "My-Header-2" to "my-value-2"),
            )
        }
        if (supportsCustomHeaders) {
            val session = connectWithTestHeaders()
            try {
                val echoedHeadersFrame = session.expectTextFrame("header info frame")
                val headers = echoedHeadersFrame.text.lines()
                assertContains(headers, "My-Header-1=my-value-1")
                assertContains(headers, "My-Header-2=my-value-2")
            } finally {
                session.close()
            }
        } else {
            assertFailsWith<IllegalArgumentException> { connectWithTestHeaders() }
        }
    }

    @Test
    fun testEchoText() = runSuspendingTest {
        val session = wsClient.connect(testServerConfig.wsUrl)

        try {
            session.sendText("hello")
            val helloResponse = session.expectTextFrame("hello frame")
            assertEquals("hello", helloResponse.text)
        } finally {
            session.close()
        }

        session.expectCloseFrame("after echo text")
        session.expectNoMoreFrames("after echo text CLOSE frame")
    }

    @Test
    fun testEchoBinary() = runSuspendingTest {
        val session = wsClient.connect(testServerConfig.wsUrl)

        try {
            val fortyTwos = ByteString(42, 42, 42)
            session.sendBinary(fortyTwos)
            val fortyTwosResponse = session.expectBinaryFrame("3 binary 42s")
            assertEquals(fortyTwos, fortyTwosResponse.bytes)
        } finally {
            session.close()
        }

        session.expectCloseFrame("after echo binary")
        session.expectNoMoreFrames("after echo binary CLOSE frame")
    }
}
