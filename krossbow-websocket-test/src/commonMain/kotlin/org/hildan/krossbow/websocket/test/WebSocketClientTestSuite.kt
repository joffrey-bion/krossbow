package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.io.bytestring.*
import org.hildan.krossbow.websocket.*
import kotlin.coroutines.*
import kotlin.test.*
import kotlin.time.*
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
    fun testConnectFailure_failsOnUnresolvedHost() = runTestRealTime {
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
    fun testConnectFailure_correctStatusCodeInException_200() = runTestRealTime {
        assertCorrectStatusReported(200) // 200 is not good for WS, should be 101
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_201_to_208() = runTestRealTime {
        assertCorrectStatusesReported(201..208) // 2xx is not good for WS, should be 101
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_301() = runTestRealTime {
        assertCorrectStatusReported(301)
    }

    // This range is broken down to avoid exceeding the test timeout and also helps to see issues with specific codes
    @Test
    fun testConnectFailure_correctStatusCodeInException_302_to_305() = runTestRealTime {
        assertCorrectStatusesReported(302..305)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_306_to_308() = runTestRealTime {
        assertCorrectStatusesReported(306..308)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_400() = runTestRealTime {
        assertCorrectStatusReported(400)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_401() = runTestRealTime {
        assertCorrectStatusReported(401)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_402() = runTestRealTime {
        assertCorrectStatusReported(402)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_403() = runTestRealTime {
        assertCorrectStatusReported(403)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_404() = runTestRealTime {
        assertCorrectStatusReported(404)
    }

    // This range is broken down to avoid exceeding the test timeout and also helps to see issues with specific codes
    @Test
    fun testConnectFailure_correctStatusCodeInException_405_to_408() = runTestRealTime {
        assertCorrectStatusesReported(405..408)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_409_to_412() = runTestRealTime {
        assertCorrectStatusesReported(409..412)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_413_to_415() = runTestRealTime {
        assertCorrectStatusesReported(413..415)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_416_to_418() = runTestRealTime {
        assertCorrectStatusesReported(416..418)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_500() = runTestRealTime {
        assertCorrectStatusReported(500)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_501() = runTestRealTime {
        assertCorrectStatusReported(501)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_502() = runTestRealTime {
        assertCorrectStatusReported(502)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_503() = runTestRealTime {
        assertCorrectStatusReported(503)
    }

    // This range is broken down to avoid exceeding the test timeout and also helps to see issues with specific codes
    @Test
    fun testConnectFailure_correctStatusCodeInException_504_to_506() = runTestRealTime {
        assertCorrectStatusesReported(504..506)
    }
    @Test
    fun testConnectFailure_correctStatusCodeInException_507_to_508() = runTestRealTime {
        assertCorrectStatusesReported(507..508)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_510() = runTestRealTime {
        assertCorrectStatusReported(510)
    }

    @Test
    fun testConnectFailure_correctStatusCodeInException_511() = runTestRealTime {
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
    fun testHandshakeHeaders() = runTestRealTime {
        val connectWithTestHeaders = suspend {
            wsClient.connect(
                url = "${testServerConfig.wsUrl}/echoHeaders",
                headers = mapOf("My-Header-1" to "my-value-1", "My-Header-2" to "my-value-2"),
            )
        }
        if (supportsCustomHeaders) {
            val session = connectWithTestHeaders()
            try {
                // for some reason, this can be pretty long with the Ktor/JS client in nodeJS tests on macOS
                val echoedHeadersFrame = session.expectTextFrame("header info frame", 30.seconds)
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
    fun testEchoText() = runTestRealTime {
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
    fun testEchoBinary() = runTestRealTime {
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

/**
 * Behaves like [runTest] but without virtual time capabilities, so we can interact with real systems like the test
 * server without instant timeouts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun runTestRealTime(
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: Duration = 60.seconds, // same default as runTest
    block: suspend CoroutineScope.() -> Unit,
) = runTest(timeout = timeout) {
    // Switches to a regular dispatcher to avoid the virtual time from runTest.
    // We also use limitedParallelism to avoid 
    withContext(Dispatchers.Default.limitedParallelism(1) + context) {
        block()
    }
}
