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
import org.hildan.krossbow.testutils.ImmediatelyFailingWebSocketClient
import org.hildan.krossbow.testutils.ImmediatelySucceedingWebSocketClient
import org.hildan.krossbow.testutils.ManuallyConnectingWebSocketClient
import org.hildan.krossbow.testutils.MockWebSocketSession
import org.hildan.krossbow.testutils.runAsyncTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class StompClientConnectionTests {

    lateinit var wsSession: MockWebSocketSession

    @BeforeTest
    fun setup() {
        wsSession = MockWebSocketSession()
    }

    @Test
    fun connect_suspendsUntilConnectedFrame() = runAsyncTest {
        val webSocketClient = ManuallyConnectingWebSocketClient()
        val stompClient = StompClient(webSocketClient)

        val session = async { stompClient.connect("dummy") }
        delay(1) // allows async call to reach suspension
        assertFalse(session.isCompleted, "connect() call should wait for web socket connection")

        webSocketClient.simulateConnectionSuccess(wsSession)
        delay(1)
        assertFalse(session.isCompleted, "connect() call should wait for STOMP connection")

        val dummyConnectedFrame = StompFrame.Connected(StompConnectedHeaders("1.2"))
        wsSession.listener.onTextMessage(dummyConnectedFrame.encodeToText())

        val awaitedSession = withTimeoutOrNull(1000) { session.await() }
        assertNotNull(awaitedSession, "connect() call should finish after receiving CONNECTED frame")
    }

    @Test
    fun connect_timesOutIfWebSocketDoesNotConnect() = runAsyncTest {
        // this client will suspend on connect() until manually triggered (which is not done during this test)
        val webSocketClient = ManuallyConnectingWebSocketClient()
        val stompClient = StompClient(webSocketClient) {
            connectionTimeoutMillis = 100
        }
        // tested code should fail before this timeout
        withTimeout(120) {
            assertFailsWith(ConnectionTimeout::class) {
                stompClient.connect("dummy")
            }
        }
    }

    @Test
    fun connect_timesOutIfConnectedFrameIsNotReceived() = runAsyncTest {
        val webSocketClient = ImmediatelySucceedingWebSocketClient(wsSession)
        val stompClient = StompClient(webSocketClient) {
            connectionTimeoutMillis = 100
        }
        // tested code should fail before this timeout
        withTimeout(120) {
            assertFailsWith(ConnectionTimeout::class) {
                stompClient.connect("dummy")
            }
        }
    }

    @Test
    fun connect_failsIfWebSocketFailsToConnect() = runAsyncTest {
        val webSocketConnectionException = Exception("some web socket exception")
        val webSocketClient = ImmediatelyFailingWebSocketClient(webSocketConnectionException)
        val stompClient = StompClient(webSocketClient)
        // the assertion is wrapped in a timeout in case the tested code suspends forever
        withTimeout(120) {
            val exception = assertFailsWith(ConnectionException::class) {
                stompClient.connect("dummy")
            }
            assertNotNull(exception.cause, "ConnectException should have original exception as cause")
            assertEquals(webSocketConnectionException::class, exception.cause!!::class)
            assertEquals(webSocketConnectionException.message, exception.cause?.message)
        }
    }

    @Test
    fun connect_failsIfErrorFrameReceived() = runAsyncTest {
        val webSocketClient = ImmediatelySucceedingWebSocketClient(wsSession)
        val stompClient = StompClient(webSocketClient)

        supervisorScope { // prevents exception to fail the test
            val session = async { stompClient.connect("dummy") }
            delay(1) // allows async call to reach suspension

            val dummyConnectedFrame = StompFrame.Error(StompErrorHeaders("connection failed"), null)
            wsSession.listener.onTextMessage(dummyConnectedFrame.encodeToText())

            withTimeout(120) {
                val exception = assertFailsWith(ConnectionException::class) {
                    session.await()
                }
                assertNotNull(exception.cause, "ConnectException should have original exception as cause")
            }
        }
    }
}
