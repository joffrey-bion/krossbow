package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.test.assertTimesOutWith
import org.hildan.krossbow.test.simulateConnectedFrameReceived
import org.hildan.krossbow.test.simulateErrorFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import org.hildan.krossbow.websocket.test.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val TEST_CONNECTION_TIMEOUT: Duration = 500.milliseconds

class StompClientTest {

    @Test
    fun connect_suspendsUntilConnectedFrame() = runBlockingTest {
        val wsClient = ControlledWebSocketClientMock()
        val stompClient = StompClient(wsClient)

        val deferredStompSession = async { stompClient.connect("dummy") }
        wsClient.waitForConnectCall()
        assertFalse(deferredStompSession.isCompleted, "connect() call should wait for web socket connection")

        val wsSession = WebSocketConnectionMock()
        wsClient.simulateSuccessfulConnection(wsSession)
        wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
        assertFalse(deferredStompSession.isCompleted, "connect() call should wait for STOMP connection")

        wsSession.simulateConnectedFrameReceived()
        assertTrue(deferredStompSession.isCompleted, "connect() call should finish after receiving CONNECTED frame")
    }

    @Test
    fun connect_sendsCorrectHeaders_fullHttpUrl() = runBlockingTest {
        testConnectHeaders(StompConnectHeaders(host = "some.host", heartBeat = HeartBeat())) { client ->
            client.connect("http://some.host:8080/ws")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_standardWsUrl() = runBlockingTest {
        testConnectHeaders(StompConnectHeaders(host = "some.host", heartBeat = HeartBeat())) { client ->
            client.connect("ws://some.host/socket")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCredentials() = runBlockingTest {
        val expectedHeaders = StompConnectHeaders(
            host = "some.host", login = "login", passcode = "pass", heartBeat = HeartBeat()
        )
        testConnectHeaders(expectedHeaders) { client ->
            client.connect("http://some.host/ws", "login", "pass")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCustomHeartBeats() = runBlockingTest {
        val customHeartBeat = HeartBeat(10.milliseconds, 50.milliseconds)
        val expectedHeaders = StompConnectHeaders(
            host = "some.host",
            heartBeat = customHeartBeat
        )
        testConnectHeaders(
            expectedHeaders = expectedHeaders,
            configureClient = { heartBeat = customHeartBeat },
        ) { client ->
            client.connect("http://some.host/ws")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCustomHeaders() {
        val userProvidedHeaders = mapOf("Authorization" to "Bearer -jwt-")
        runBlockingTest {
            val expectedHeaders = StompConnectHeaders(
                host = "some.host",
                heartBeat = HeartBeat(),
                customHeaders = userProvidedHeaders
            )
            testConnectHeaders(expectedHeaders) { client ->
                client.connect("http://some.host/ws", customStompConnectHeaders = userProvidedHeaders)
            }
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCustomHostHeader() {
        runBlockingTest {
            val expectedHeaders = StompConnectHeaders(
                host = "custom",
                heartBeat = HeartBeat(),
            )
            testConnectHeaders(expectedHeaders) { client ->
                client.connect("http://some.host/ws", host = "custom")
            }
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withNoHostHeader() {
        runBlockingTest {
            val expectedHeaders = StompConnectHeaders(
                host = null,
                heartBeat = HeartBeat(),
            )
            testConnectHeaders(expectedHeaders) { client ->
                client.connect("http://some.host/ws", host = null)
            }
        }
    }

    private suspend fun testConnectHeaders(
        expectedHeaders: StompConnectHeaders,
        configureClient: StompConfig.() -> Unit = {},
        connectCall: suspend (StompClient) -> Unit,
    ) {
        coroutineScope {
            val wsSession = WebSocketConnectionMock()
            val stompClient = StompClient(webSocketClientMock { wsSession }, configureClient)

            launch { connectCall(stompClient) }
            val frame = wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
            assertEquals<Map<String, String>>(HashMap(expectedHeaders), HashMap(frame.headers))
            assertNull(frame.body, "connect frame should not have a body")

            // just to end the connect call
            wsSession.simulateConnectedFrameReceived()
        }
    }

    @Test
    fun connect_timesOutIfWebSocketDoesNotConnect() = runBlockingTest {
        // this WS client will suspend on connect() until manually triggered (which is not done during this test)
        val stompClient = StompClient(ControlledWebSocketClientMock()) {
            connectionTimeout = TEST_CONNECTION_TIMEOUT
        }
        // we don't expect the closure of the web socket here, because it has not even been connected at all
        assertTimesOutWith(ConnectionTimeout::class, TEST_CONNECTION_TIMEOUT) {
            stompClient.connect("dummy")
        }
    }

    @Test
    fun connect_timesOutIfConnectedFrameIsNotReceived() = runBlockingTest {
        val wsSession = WebSocketConnectionMock()
        val stompClient = StompClient(webSocketClientMock { wsSession }) {
            connectionTimeout = TEST_CONNECTION_TIMEOUT
        }
        launch {
            // should close WS on timeout to avoid leaking it (connection at WS level was done, just not at STOMP level)
            wsSession.expectClose()
        }
        assertTimesOutWith(ConnectionTimeout::class, TEST_CONNECTION_TIMEOUT) {
            stompClient.connect("dummy")
        }
    }

    @Test
    fun connect_failsIfWebSocketFailsToConnect() = runBlockingTest {
        val wsConnectionException = Exception("some web socket exception")
        val stompClient = StompClient(webSocketClientMock { throw wsConnectionException })
        val exception = assertFailsWith(WebSocketConnectionException::class) {
            stompClient.connect("wss://dummy.com")
        }
        assertNotNull(exception.cause, "ConnectException should have original exception as cause")
        assertEquals("wss://dummy.com", exception.url)
        assertEquals(wsConnectionException::class, exception.cause!!::class)
        assertEquals(wsConnectionException.message, exception.cause?.message)
    }

    @Test
    fun connect_failsIfErrorFrameReceived() = runBlockingTest {
        val wsSession = WebSocketConnectionMock()
        val stompClient = StompClient(webSocketClientMock { wsSession })

        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
            wsSession.simulateErrorFrameReceived("connection failed")
            wsSession.expectClose()
        }

        val exception = assertFailsWith(StompConnectionException::class) {
            stompClient.connect("wss://dummy.com/path")
        }
        assertEquals("dummy.com", exception.host)
        assertNotNull(exception.cause, "StompConnectionException should have original exception as cause")
    }

    @Test
    fun connect_failsIfWebSocketIsClosedWhenConnecting() = runBlockingTest {
        val wsSession = WebSocketConnectionMock()
        val stompClient = StompClient(webSocketClientMock { wsSession })

        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
            wsSession.simulateClose(1000, "abrupt close")
            wsSession.expectNoClose()
        }

        val exception = assertFailsWith(StompConnectionException::class) {
            stompClient.connect("wss://dummy.com/path")
        }
        assertEquals("dummy.com", exception.host)
        assertNotNull(exception.cause, "StompConnectionException should have original exception as cause")
    }

    @Test
    fun connect_shouldNotLeakWebSocketConnectionIfCancelled() = runBlockingTest {
        val wsSession = WebSocketConnectionMock()
        val stompClient = StompClient(webSocketClientMock { wsSession })

        val connectJob = launch {
            stompClient.connect("wss://dummy.com/path")
        }
        // ensures we have already connected the WS
        wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
        // simulates the cancellation of the connect() call during the STOMP connect handshake
        connectJob.cancel()
        wsSession.expectClose()
    }

    @Test
    fun stomp_shouldCloseWebSocketConnectionIfCancelled() = runBlockingTest {
        val wsSession = WebSocketConnectionMock()

        val connectJob = launch {
            wsSession.stomp(StompConfig())
        }
        // ensures we have already connected the WS, and have started the STOMP handshake
        wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
        // simulates the cancellation of the stomp() call during the STOMP connect handshake
        connectJob.cancel()
        wsSession.expectClose()
    }

    @Test
    fun errorOnWebSocketShouldCloseTheSession_messageBiggerThanCloseReasonMaxLength() = runBlockingTest {
        val errorMessage = "some web socket exception with a very long message that exceeds the maximum " +
            "allowed length for the 'reason' in web socket close frames. It needs to be truncated."
        val wsSession = WebSocketConnectionMock()
        val stompClient = StompClient(webSocketClientMock { wsSession }) {
            // necessary so runBlockingTest knows to wait for the session's coroutines to progress
            defaultSessionCoroutineContext = coroutineContext[ContinuationInterceptor.Key]!! // retrieves the test dispatcher
        }

        launch {
            stompClient.connect("wss://dummy.com")
        }
        wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
        wsSession.simulateConnectedFrameReceived()
        wsSession.simulateErrorFrameReceived(errorMessage)
        val closeEvent = wsSession.expectClose()
        val reason = closeEvent.reason
        assertNotNull(reason, "Close reason should be present")
        assertTrue(errorMessage.startsWith(reason), "Close reason should be based on the error message")
        assertTrue(reason.encodeToByteArray().size <= 123, "Reason should be truncated to 123 bytes")
    }
}
