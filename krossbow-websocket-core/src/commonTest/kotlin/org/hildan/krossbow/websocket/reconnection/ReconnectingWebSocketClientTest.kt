package org.hildan.krossbow.websocket.reconnection

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.*
import kotlin.coroutines.*
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReconnectingWebSocketClientTest {

    @Test
    fun shouldConnectSuccessfully() = runTest {
        val baseClient = WebSocketClientMock()
        val reconnectingClient = baseClient.withAutoReconnect { reconnectContext = testDispatcher }

        val callUrl = "dummy"
        val callProtocols = listOf("v10.stomp", "other")
        val callHeaders = mapOf("my-header" to "my-value")

        val deferredConnection = async {
            reconnectingClient.connect(url = callUrl, protocols = callProtocols, headers = callHeaders)
        }

        val connectCall = baseClient.awaitConnectCall()
        assertEquals(WebSocketConnectCall(url = callUrl, protocols = callProtocols, headers = callHeaders), connectCall)
        advanceUntilIdle()
        assertFalse(deferredConnection.isCompleted, "connect() call should suspend until base client connects")

        baseClient.simulateSuccessfulConnection(WebSocketConnectionMock())
        val connection = withTimeoutOrNull(50) { deferredConnection.await() }
        assertNotNull(connection, "connect() call should resume after base client connects")
    }

    @Test
    fun shouldFailConnectionWhenBaseClientFails() = runTest {
        val baseClient = WebSocketClientMock()
        val reconnectingClient = baseClient.withAutoReconnect { reconnectContext = testDispatcher }

        launch {
            baseClient.awaitConnectAndSimulateFailure(Exception("something bad"))
        }
        val exception = assertFailsWith(Exception::class) {
            reconnectingClient.connect("dummy")
        }
        assertEquals("something bad", exception.message)
    }

    @Test
    fun shouldForwardFrames() = runTest {
        val baseClient = WebSocketClientMock()
        val reconnectingClient = baseClient.withAutoReconnect { reconnectContext = testDispatcher }

        launch {
            val baseConnection = baseClient.awaitConnectAndSimulateSuccess()
            baseConnection.simulateTextFrameReceived("test")
        }
        val connection = reconnectingClient.connect("dummy")
        val received = connection.expectTextFrame("test frame")
        assertEquals(received.text, "test", "The message of the forwarded frame should match the received frame")
    }

    @Test
    fun shouldCompleteNormallyWhenUnderlyingFramesFlowCompletes() = runTest {
        val baseClient = WebSocketClientMock()
        val reconnectingClient = baseClient.withAutoReconnect { reconnectContext = testDispatcher }

        launch {
            val baseConnection = baseClient.awaitConnectAndSimulateSuccess()
            baseConnection.simulateClose(code = WebSocketCloseCodes.NORMAL_CLOSURE, reason = "some reason")
        }
        val connection = reconnectingClient.connect("dummy")
        val received = connection.expectCloseFrame()
        assertEquals(received.code, WebSocketCloseCodes.NORMAL_CLOSURE, "The close code should match")
        assertEquals(received.reason, "some reason", "The close reason should match")
        connection.expectNoMoreFrames()
    }

    @Test
    fun shouldReconnectAndForwardFramesFromNewConnection() = runTest {
        val baseClient = WebSocketClientMock()
        val reconnectingClient = baseClient.withAutoReconnect {
            reconnectContext = testDispatcher
            delayStrategy = FixedDelay(1.milliseconds)
        }

        launch {
            val conn0 = baseClient.awaitConnectAndSimulateSuccess()
            conn0.simulateTextFrameReceived("test1")
            conn0.simulateError("simulated error")

            val conn1 = baseClient.awaitConnectAndSimulateSuccess()
            conn1.simulateTextFrameReceived("test2")
        }

        val connection = reconnectingClient.connect("dummy")
        val received1 = connection.expectTextFrame("test frame 1")
        assertEquals(received1.text, "test1", "The message of the forwarded frame should match the received frame")
        val received2 = connection.expectTextFrame("frame2")
        assertEquals(received2.text, "test2", "The message of the forwarded frame should match the received frame")
    }

    @Test
    fun shouldCallReconnectCallbackWhenReconnected() = runTest {
        val baseClient = WebSocketClientMock()

        var reconnected = false
        val reconnectingClient = baseClient.withAutoReconnect {
            reconnectContext = testDispatcher
            delayStrategy = FixedDelay(1.milliseconds)
            afterReconnect {
                reconnected = true
            }
        }
        
        launch {
            val connection = reconnectingClient.connect("dummy")
            val received1 = connection.expectTextFrame("test frame")
            assertEquals(received1.text, "test1", "The message of the forwarded frame should match the received frame")
        }
        val conn0 = baseClient.awaitConnectAndSimulateSuccess()
        conn0.simulateTextFrameReceived("test1")
        
        advanceUntilIdle() // give time to potential reconnect coroutine
        assertFalse(reconnected, "afterReconnect callback should not be called after first connection")
        
        conn0.simulateError("simulated error")
        
        advanceUntilIdle() // give time to potential reconnect coroutine
        assertFalse(reconnected, "afterReconnect callback should not be called until the reconnection happened")

        baseClient.awaitConnectAndSimulateSuccess()
        advanceUntilIdle() // give time to trigger reconnect coroutine
        assertTrue(reconnected, "afterReconnect callback should be called")
    }

    @Test
    fun shouldFailAfterMaxAttempts() = runTest {
        val baseClient = WebSocketClientMock()

        var reconnected = false
        val reconnectingClient = baseClient.withAutoReconnect {
            reconnectContext = testDispatcher
            maxAttempts = 5
            delayStrategy = FixedDelay(100.milliseconds)
            afterReconnect {
                reconnected = true
            }
        }

        launch {
            val baseConnection = baseClient.awaitConnectAndSimulateSuccess()
            baseConnection.simulateTextFrameReceived("test1")
            baseConnection.simulateError("simulated error")
            repeat(5) {
                baseClient.awaitConnectAndSimulateFailure(RuntimeException("error $it"))
            }
        }

        val connection = reconnectingClient.connect("dummy")
        val received1 = connection.expectTextFrame("test frame")
        assertEquals(received1.text, "test1", "The message of the forwarded frame should match the received frame")

        advanceUntilIdle() // give time to trigger reconnect coroutine

        val exception = assertFailsWith(WebSocketReconnectionException::class) { connection.incomingFrames.first() }
        val cause = exception.cause
        assertNotNull(cause, "should provide the last attempt's exception as cause")
        assertEquals("error 4", cause.message)
        assertFalse(reconnected, "afterReconnect callback should not be called")
    }

    @Test
    fun shouldFailIfReconnectPredicateIsFalse() = runTest {
        val baseClient = WebSocketClientMock()

        var reconnected = false
        val reconnectingClient = baseClient.withAutoReconnect {
            reconnectContext = testDispatcher
            maxAttempts = 5
            delayStrategy = FixedDelay(100.milliseconds)
            reconnectWhen { _, _ -> false }
            afterReconnect {
                reconnected = true
            }
        }

        launch {
            val baseConnection = baseClient.awaitConnectAndSimulateSuccess()
            baseConnection.simulateTextFrameReceived("test1")
            baseConnection.simulateError("simulated error")
        }

        val connection = reconnectingClient.connect("dummy")
        val received1 = connection.expectTextFrame("test frame")
        assertEquals(received1.text, "test1", "The message of the forwarded frame should match the received frame")

        advanceUntilIdle() // give time to trigger reconnect coroutine

        val exception = assertFailsWith(WebSocketException::class) { connection.incomingFrames.first() }
        assertEquals("simulated error", exception.message)
        assertFalse(reconnected, "afterReconnect callback should not be called")
    }

    @Test
    fun shouldFailWhenReconnectPredicateBecomesFalse() = runTest {
        val baseClient = WebSocketClientMock()
        val reconnectingClient = baseClient.withAutoReconnect {
            reconnectContext = testDispatcher
            delayStrategy = FixedDelay(100.milliseconds)
            reconnectWhen { _, attempt -> attempt < 2 }
        }

        launch {
            val baseConnection = baseClient.awaitConnectAndSimulateSuccess()
            baseConnection.simulateTextFrameReceived("test1")
            baseConnection.simulateError("simulated error")
            baseClient.awaitConnectAndSimulateFailure(RuntimeException("connection failure 1"))
            baseClient.awaitConnectAndSimulateFailure(RuntimeException("connection failure 2"))
        }

        val connection = reconnectingClient.connect("dummy")
        val received1 = connection.expectTextFrame("test frame 1")
        assertEquals(received1.text, "test1", "The message of the forwarded frame should match the received frame (1)")

        advanceUntilIdle() // give time to trigger reconnect coroutine

        val exception = assertFailsWith(RuntimeException::class) { connection.incomingFrames.first() }
        assertEquals("connection failure 2", exception.message)
    }
}

private val TestScope.testDispatcher
    get() = coroutineContext[ContinuationInterceptor] ?: EmptyCoroutineContext
