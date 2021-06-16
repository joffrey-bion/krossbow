package org.hildan.krossbow.stomp

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
import kotlin.test.*

private const val TEST_CONNECTION_TIMEOUT: Long = 500

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
        val customHeartBeat = HeartBeat(10, 50)
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
            connectionTimeoutMillis = TEST_CONNECTION_TIMEOUT
        }
        assertTimesOutWith(ConnectionTimeout::class, TEST_CONNECTION_TIMEOUT) {
            stompClient.connect("dummy")
        }
    }

    @Test
    fun connect_timesOutIfConnectedFrameIsNotReceived() = runBlockingTest {
        val stompClient = StompClient(webSocketClientMock()) {
            connectionTimeoutMillis = TEST_CONNECTION_TIMEOUT
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
            stompClient.connect("dummy")
        }
        assertNotNull(exception.cause, "ConnectException should have original exception as cause")
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
            stompClient.connect("dummy")
        }
        assertNotNull(exception.cause, "StompConnectionException should have original exception as cause")
    }
}
