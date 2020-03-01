package org.hildan.krossbow.stomp

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.test.ImmediatelyFailingWebSocketClient
import org.hildan.krossbow.test.ImmediatelySucceedingWebSocketClient
import org.hildan.krossbow.test.ManuallyConnectingWebSocketClient
import org.hildan.krossbow.test.WebSocketSessionMock
import org.hildan.krossbow.test.assertCompletesSoon
import org.hildan.krossbow.test.assertTimesOutWith
import org.hildan.krossbow.test.runAsyncTest
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.test.simulateConnectedFrameReceived
import org.hildan.krossbow.test.simulateErrorFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val TEST_CONNECTION_TIMEOUT: Long = 20

class StompClientTest {

    @Test
    fun connect_suspendsUntilConnectedFrame() = runAsyncTestWithTimeout {
        val wsClient = ManuallyConnectingWebSocketClient()
        val stompClient = StompClient(wsClient)

        val deferredStompSession = async { stompClient.connect("dummy") }
        wsClient.waitForConnectCall()
        assertFalse(deferredStompSession.isCompleted, "connect() call should wait for web socket connection")

        val wsSession = WebSocketSessionMock()
        wsClient.simulateSuccessfulConnection(wsSession)
        wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
        assertFalse(deferredStompSession.isCompleted, "connect() call should wait for STOMP connection")

        wsSession.simulateConnectedFrameReceived()
        assertCompletesSoon(deferredStompSession, "connect() call should finish after receiving CONNECTED frame")
    }

    @Test
    fun connect_sendsCorrectHeaders_fullHttpUrl() = runAsyncTestWithTimeout {
        testConnectHeaders(StompConnectHeaders(host = "some.host", heartBeat = HeartBeat())) { client ->
            client.connect("http://some.host:8080/ws")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_standardWsUrl() = runAsyncTestWithTimeout {
        testConnectHeaders(StompConnectHeaders(host = "some.host", heartBeat = HeartBeat())) { client ->
            client.connect("ws://some.host/socket")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCredentials() = runAsyncTestWithTimeout {
        val expectedHeaders = StompConnectHeaders(
            host = "some.host", login = "login", passcode = "pass", heartBeat = HeartBeat()
        )
        testConnectHeaders(expectedHeaders) { client ->
            client.connect("http://some.host/ws", "login", "pass")
        }
    }

    @Test
    fun connect_sendsCorrectHeaders_withCustomHeartBeats() = runAsyncTestWithTimeout {
        val customHeartBeat = HeartBeat(10, 50)
        val expectedHeaders = StompConnectHeaders(
            host = "some.host",
            heartBeat = customHeartBeat
        )
        testConnectHeaders(
            expectedHeaders = expectedHeaders,
            configureClient = { heartBeat = customHeartBeat }
        ) { client ->
            client.connect("http://some.host/ws")
        }
    }

    private suspend fun testConnectHeaders(
        expectedHeaders: StompConnectHeaders,
        configureClient: StompConfig.() -> Unit = {},
        connectCall: suspend (StompClient) -> Unit
    ) {
        coroutineScope {
            val wsSession = WebSocketSessionMock()
            val stompClient = StompClient(ImmediatelySucceedingWebSocketClient(wsSession), configureClient)

            launch { connectCall(stompClient) }
            val frame = wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
            assertEquals(HashMap(expectedHeaders), HashMap(frame.headers))
            assertNull(frame.body, "connect frame should not have a body")

            // just to end the connect call
            wsSession.simulateConnectedFrameReceived()
        }
    }

    @Test
    fun connect_timesOutIfWebSocketDoesNotConnect() = runAsyncTest {
        // this WS client will suspend on connect() until manually triggered (which is not done during this test)
        val stompClient = StompClient(ManuallyConnectingWebSocketClient()) {
            connectionTimeoutMillis = TEST_CONNECTION_TIMEOUT
        }
        assertTimesOutWith(ConnectionTimeout::class, TEST_CONNECTION_TIMEOUT) {
            stompClient.connect("dummy")
        }
    }

    @Test
    fun connect_timesOutIfConnectedFrameIsNotReceived() = runAsyncTest {
        val stompClient = StompClient(ImmediatelySucceedingWebSocketClient()) {
            connectionTimeoutMillis = TEST_CONNECTION_TIMEOUT
        }
        assertTimesOutWith(ConnectionTimeout::class, TEST_CONNECTION_TIMEOUT) {
            stompClient.connect("dummy")
        }
    }

    @Test
    fun connect_failsIfWebSocketFailsToConnect() = runAsyncTestWithTimeout {
        val wsConnectionException = Exception("some web socket exception")
        val stompClient = StompClient(ImmediatelyFailingWebSocketClient(wsConnectionException))
        val exception = assertFailsWith(ConnectionException::class) {
            stompClient.connect("dummy")
        }
        assertNotNull(exception.cause, "ConnectException should have original exception as cause")
        assertEquals(wsConnectionException::class, exception.cause!!::class)
        assertEquals(wsConnectionException.message, exception.cause?.message)
    }

    @Test
    fun connect_failsIfErrorFrameReceived() = runAsyncTestWithTimeout {
        // prevents the async connect() exception from failing the test
        supervisorScope {
            val wsSession = WebSocketSessionMock()
            val stompClient = StompClient(ImmediatelySucceedingWebSocketClient(wsSession))

            val stompSession = async { stompClient.connect("dummy") }
            wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)

            // This method call should not fail because calling this listener is the responsibility of the web socket
            // implementation, and is only meant to inform the STOMP implementation about that received frame, so we
            // don't want that to fail. What we do want to fail is the pending client calls (like connect in this case)
            wsSession.simulateErrorFrameReceived("connection failed")

            val exception = assertFailsWith(ConnectionException::class) {
                stompSession.await()
            }
            assertNotNull(exception.cause, "ConnectException should have original exception as cause")
        }
    }
}
