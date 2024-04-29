package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.headers.*
import org.hildan.krossbow.test.*
import org.hildan.krossbow.websocket.test.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val TEST_CONNECTION_TIMEOUT: Duration = 500.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class StompClientTest {

    @Test
    fun connect_translatesToCorrectWebSocketConnect() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient)

        val deferredStompSession = async { stompClient.connect("ws://dummy") }

        val connectCall = wsClient.awaitConnectCall()
        assertEquals("ws://dummy", connectCall.url)
        assertEquals(listOf("v12.stomp", "v11.stomp", "v10.stomp"), connectCall.protocols)
        assertEquals(emptyMap(), connectCall.headers)
        
        deferredStompSession.cancelAndJoin()
    }

    @Test
    fun connect_suspendsUntilConnectedFrame() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient)

        val deferredStompSession = async { stompClient.connect("ws://dummy") }
        wsClient.awaitConnectCall()
        assertFalse(deferredStompSession.isCompleted, "connect() call should wait for web socket connection")

        val wsSession = WebSocketConnectionMock()
        wsClient.simulateSuccessfulConnection(wsSession)
        wsSession.awaitConnectFrameAndSimulateCompletion()
        assertFalse(deferredStompSession.isCompleted, "connect() call should wait for STOMP connection")

        wsSession.simulateConnectedFrameReceived()
        advanceUntilIdle()
        assertTrue(deferredStompSession.isCompleted, "connect() call should finish after receiving CONNECTED frame")
    }

    @Test
    fun connect_wsSubprotocolNegotiation_hostNotSentInV10() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient)

        launch { stompClient.connect("ws://dummy") }

        val wsSession = wsClient.awaitConnectAndSimulateSuccess(selectedProtocol = "v10.stomp")

        val connectFrame = wsSession.awaitConnectFrameAndSimulateCompletion()
        assertNull(connectFrame.headers.host, "should not send 'host' header in STOMP 1.0")

        wsSession.simulateConnectedFrameReceived(StompConnectedHeaders(version = "1.0"))
    }

    @Test
    fun connect_wsSubprotocolNegotiation_failOnVersionMismatch() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient)

        launch {
            val wsSession = wsClient.awaitConnectAndSimulateSuccess(selectedProtocol = "v11.stomp")
            wsSession.awaitConnectFrameAndSimulateCompletion()
            wsSession.simulateConnectedFrameReceived(StompConnectedHeaders(version = "1.2"))
        }

        val exception = assertFailsWith<StompConnectionException> {
            stompClient.connect("ws://dummy")
        }
        assertEquals("negotiated STOMP version mismatch: 1.1 at web socket level (subprotocol 'v11.stomp'), 1.2 at STOMP level", exception.cause?.message)
    }

    @Test
    fun connect_wsSubprotocolNegotiation_noFailOnVersionMismatch() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient) {
            failOnStompVersionMismatch = false
        }

        launch {
            val wsSession = wsClient.awaitConnectAndSimulateSuccess(selectedProtocol = "v11.stomp")
            wsSession.awaitConnectFrameAndSimulateCompletion()
            wsSession.simulateConnectedFrameReceived(StompConnectedHeaders(version = "1.2"))
            wsSession.expectClose()
        }

        val session = stompClient.connect("ws://dummy") // should not fail
        session.disconnect()
    }

    @Test
    fun connect_sendsCorrectHeaders_fullHttpUrl() = runTest {
        testConnectHeaders(StompConnectHeaders(host = "some.host", heartBeat = HeartBeat())) { client ->
            client.connect("http://some.host:8080/ws")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_standardWsUrl() = runTest {
        testConnectHeaders(StompConnectHeaders(host = "some.host", heartBeat = HeartBeat())) { client ->
            client.connect("ws://some.host/socket")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCredentials() = runTest {
        val expectedHeaders = StompConnectHeaders(
            host = "some.host", login = "login", passcode = "pass", heartBeat = HeartBeat()
        )
        testConnectHeaders(expectedHeaders) { client ->
            client.connect("http://some.host/ws", "login", "pass")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCustomHeartBeats() = runTest {
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
    fun connect_sendsCorrectHeaders_withCustomHeaders() = runTest {
        val userProvidedHeaders = mapOf("Authorization" to "Bearer -jwt-")
        val expectedHeaders = StompConnectHeaders(
            host = "some.host",
            heartBeat = HeartBeat(),
            customHeaders = userProvidedHeaders
        )
        testConnectHeaders(expectedHeaders) { client ->
            client.connect("http://some.host/ws", customStompConnectHeaders = userProvidedHeaders)
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCustomHostHeader() = runTest {
        val expectedHeaders = StompConnectHeaders(
            host = "custom",
            heartBeat = HeartBeat(),
        )
        testConnectHeaders(expectedHeaders) { client ->
            client.connect("http://some.host/ws", host = "custom")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withNoHostHeader() = runTest {
        val expectedHeaders = StompConnectHeaders(
            host = null,
            heartBeat = HeartBeat(),
        )
        testConnectHeaders(expectedHeaders) { client ->
            client.connect("http://some.host/ws", host = null)
        }
    }

    private suspend fun testConnectHeaders(
        expectedHeaders: StompConnectHeaders,
        configureClient: StompConfig.() -> Unit = {},
        connectCall: suspend (StompClient) -> StompSession,
    ) {
        coroutineScope {
            val webSocketClient = WebSocketClientMock()
            val stompClient = StompClient(webSocketClient, configureClient)

            launch {
                val session = connectCall(stompClient)
                session.disconnect()
            }
            val wsSession = webSocketClient.awaitConnectAndSimulateSuccess()
            val frame = wsSession.awaitConnectFrameAndSimulateCompletion()
            assertEquals<Map<String, String>>(HashMap(expectedHeaders), HashMap(frame.headers))
            assertNull(frame.body, "connect frame should not have a body")

            // just to end the connect call
            wsSession.simulateConnectedFrameReceived()
            wsSession.expectClose()
        }
    }

    @Test
    fun connect_timesOutIfWebSocketDoesNotConnect() = runTest {
        // this WS client will suspend on connect() until manually triggered (which is not done during this test)
        val stompClient = StompClient(WebSocketClientMock()) {
            connectionTimeout = TEST_CONNECTION_TIMEOUT
        }
        // we don't expect the closure of the web socket here, because it has not even been connected at all
        assertTimesOutWith(ConnectionTimeout::class, TEST_CONNECTION_TIMEOUT) {
            stompClient.connect("dummy")
        }
    }

    @Test
    fun connect_timesOutIfConnectedFrameIsNotReceived() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient) {
            connectionTimeout = TEST_CONNECTION_TIMEOUT
        }
        launch {
            val wsConnection = wsClient.awaitConnectAndSimulateSuccess()
            // should close WS on timeout to avoid leaking it (connection at WS level was done, just not at STOMP level)
            wsConnection.expectClose()
        }
        assertTimesOutWith(ConnectionTimeout::class, TEST_CONNECTION_TIMEOUT) {
            stompClient.connect("dummy")
        }
    }

    @Test
    fun connect_failsIfWebSocketFailsToConnect() = runTest {
        val wsConnectionException = Exception("some web socket exception")
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient)
        launch {
            wsClient.awaitConnectAndSimulateFailure(wsConnectionException)
        }
        val exception = assertFailsWith(Exception::class) {
            stompClient.connect("wss://dummy.com")
        }
        assertEquals(wsConnectionException.message, exception.message)
    }

    @Test
    fun connect_failsIfErrorFrameReceived() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient)

        launch {
            val wsSession = wsClient.awaitConnectAndSimulateSuccess()
            wsSession.awaitConnectFrameAndSimulateCompletion()
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
    fun connect_failsIfWebSocketIsClosedWhenConnecting() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient)

        launch {
            val wsSession = wsClient.awaitConnectAndSimulateSuccess()
            wsSession.awaitConnectFrameAndSimulateCompletion()
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
    fun connect_shouldNotLeakWebSocketConnectionIfCancelled() = runTest {
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient)

        val connectJob = launch {
            stompClient.connect("wss://dummy.com/path")
        }
        val wsSession = wsClient.awaitConnectAndSimulateSuccess()
        // ensures we have sent the STOMP CONNECT frame over the WS
        wsSession.awaitConnectFrameAndSimulateCompletion()
        // simulates the cancellation of the connect() call during the STOMP connect handshake
        connectJob.cancel()
        wsSession.expectClose()
    }

    @Test
    fun stomp_shouldCloseWebSocketConnectionIfCancelled() = runTest {
        val wsSession = WebSocketConnectionMock()

        val connectJob = launch {
            wsSession.stomp(StompConfig())
        }
        // ensures we have already connected the WS, and have started the STOMP handshake
        wsSession.awaitConnectFrameAndSimulateCompletion()
        // simulates the cancellation of the stomp() call during the STOMP connect handshake
        connectJob.cancel()
        wsSession.expectClose()
    }

    @Test
    fun errorOnWebSocketShouldCloseTheSession_messageBiggerThanCloseReasonMaxLength() = runTest {
        val errorMessage = "some web socket exception with a very long message that exceeds the maximum " +
            "allowed length for the 'reason' in web socket close frames. It needs to be truncated."
        val wsClient = WebSocketClientMock()
        val stompClient = StompClient(wsClient) {
            // necessary so runTest knows to wait for the session's coroutines to progress
            defaultSessionCoroutineContext = coroutineContext[ContinuationInterceptor.Key]!! // retrieves the test dispatcher
        }

        launch {
            stompClient.connect("wss://dummy.com")
        }
        val wsSession = wsClient.awaitConnectAndSimulateSuccess()
        wsSession.awaitConnectFrameAndSimulateCompletion()
        wsSession.simulateConnectedFrameReceived()
        wsSession.simulateErrorFrameReceived(errorMessage)
        val closeEvent = wsSession.expectClose()
        val reason = closeEvent.reason
        assertNotNull(reason, "Close reason should be present")
        assertTrue(errorMessage.startsWith(reason), "Close reason should be based on the error message")
        assertTrue(reason.encodeToByteArray().size <= 123, "Reason should be truncated to 123 bytes")
    }
}
