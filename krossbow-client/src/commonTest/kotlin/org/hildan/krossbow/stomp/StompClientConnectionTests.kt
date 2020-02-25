package org.hildan.krossbow.stomp

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.test.ImmediatelyFailingWebSocketClient
import org.hildan.krossbow.test.ImmediatelySucceedingWebSocketClient
import org.hildan.krossbow.test.ManuallyConnectingWebSocketClient
import org.hildan.krossbow.test.MockWebSocketSession
import org.hildan.krossbow.test.runAsyncTest
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class StompClientConnectionTests {

    @Test
    fun connect_suspendsUntilConnectedFrame() = runAsyncTestWithTimeout {
        val wsClient = ManuallyConnectingWebSocketClient()
        val stompClient = StompClient(wsClient)

        val deferredStompSession = async { stompClient.connect("dummy") }
        delay(1) // allows async call to reach suspension
        assertFalse(deferredStompSession.isCompleted, "connect() call should wait for web socket connection")

        val wsSession = MockWebSocketSession()
        wsClient.simulateConnectionSuccess(wsSession)
        delay(1)
        assertFalse(deferredStompSession.isCompleted, "connect() call should wait for STOMP connection")

        val dummyConnectedFrame = StompFrame.Connected(StompConnectedHeaders("1.2"))
        wsSession.listener.onTextMessage(dummyConnectedFrame.encodeToText())

        val awaitedSession = withTimeoutOrNull(1000) { deferredStompSession.await() }
        assertNotNull(awaitedSession, "connect() call should finish after receiving CONNECTED frame")
    }

    @Test
    fun connect_timesOutIfWebSocketDoesNotConnect() = runAsyncTest {
        // this WS client will suspend on connect() until manually triggered (which is not done during this test)
        val stompClient = StompClient(ManuallyConnectingWebSocketClient()) {
            connectionTimeoutMillis = 100
        }
        // tested code should fail (with 100ms timeout) before this 120ms timeout
        withTimeout(120) {
            assertFailsWith(ConnectionTimeout::class) {
                stompClient.connect("dummy")
            }
        }
    }

    @Test
    fun connect_timesOutIfConnectedFrameIsNotReceived() = runAsyncTest {
        val stompClient = StompClient(ImmediatelySucceedingWebSocketClient()) {
            connectionTimeoutMillis = 100
        }
        // tested code should fail (with 100ms timeout) before this 120ms timeout
        withTimeout(120) {
            assertFailsWith(ConnectionTimeout::class) {
                stompClient.connect("dummy")
            }
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
        supervisorScope { // prevents the async connect() exception from failing the test
            val wsSession = MockWebSocketSession()
            val stompClient = StompClient(ImmediatelySucceedingWebSocketClient(wsSession))

            val stompSession = async { stompClient.connect("dummy") }
            delay(1) // allows async call to reach suspension

            val errorFrame = StompFrame.Error(StompErrorHeaders("connection failed"), null)

            // This method call should not fail because calling this listener is the responsibility of the web socket
            // implementation, and is only meant to inform the STOMP implementation about that received frame, so we
            // don't want that to fail. What we do want to fail is the pending client calls (like connect in this case)
            wsSession.listener.onTextMessage(errorFrame.encodeToText())

            val exception = assertFailsWith(ConnectionException::class) {
                stompSession.await()
            }
            assertNotNull(exception.cause, "ConnectException should have original exception as cause")
        }
    }
}
