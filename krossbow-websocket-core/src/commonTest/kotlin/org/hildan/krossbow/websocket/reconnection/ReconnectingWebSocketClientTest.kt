package org.hildan.krossbow.websocket.reconnection

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.test.*
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

// limitations on Kotlin/Native multithreaded coroutines prevent the reconnection wrapper from working properly
@IgnoreOnNative
@OptIn(ExperimentalTime::class)
internal class ReconnectingWebSocketClientTest {

    @Test
    fun shouldConnectSuccessfully() = runSuspendingTest {
        val webSocketClientMock = ControlledWebSocketClientMock()
        val reconnectingClient = webSocketClientMock.withAutoReconnect()

        val deferredConnection = async { reconnectingClient.connect("dummy") }

        webSocketClientMock.waitForConnectCall()
        delay(10)
        assertFalse(deferredConnection.isCompleted, "connect() call should suspend until base client connects")

        webSocketClientMock.simulateSuccessfulConnection(WebSocketConnectionMock())
        val connection = withTimeoutOrNull(50) { deferredConnection.await() }
        assertNotNull(connection, "connect() call should resume after base client connects")
    }

    @Test
    fun shouldFailConnectionWhenBaseClientFails() = runSuspendingTest {
        val webSocketClientMock = ControlledWebSocketClientMock()
        val reconnectingClient = webSocketClientMock.withAutoReconnect()

        launch {
            webSocketClientMock.waitForConnectCall()
            webSocketClientMock.simulateFailedConnection(Exception("something bad"))
        }
        val exception = assertFailsWith(Exception::class) {
            reconnectingClient.connect("dummy")
        }
        assertEquals("something bad", exception.message)
    }

    @Test
    fun shouldForwardFrames() = runSuspendingTest {
        val baseConnection = WebSocketConnectionMock()
        val baseClient = webSocketClientMock { baseConnection }

        val reconnectingClient = baseClient.withAutoReconnect()
        val connection = reconnectingClient.connect("dummy")

        launch { baseConnection.simulateTextFrameReceived("test") }
        val received = withTimeoutOrNull(500) { connection.incomingFrames.receive() }
        assertNotNull(received, "The received frame should be forwarded")
        assertTrue(received is WebSocketFrame.Text, "The received frame should be a text frame")
        assertEquals(received.text, "test", "The message of the forwarded frame should match the received frame")
    }

    @Test
    fun shouldReconnectAndForwardFramesFromNewConnection() = runSuspendingTest {
        val connections = mutableListOf<WebSocketConnectionMock>()
        val baseClient = webSocketClientMock {
            WebSocketConnectionMock().also {
                connections.add(it) // FYI, exceptions here could be swallowed
            }
        }

        val reconnectingClient = baseClient.withAutoReconnect(delayStrategy = FixedDelay(Duration.milliseconds(1)))
        val connection = reconnectingClient.connect("dummy")
        // if this fails, maybe an exception happened in the connect() method (only visible when reading frames)
        assertEquals(1, connections.size, "base client should have provided 1 connection")

        launch { connections[0].simulateTextFrameReceived("test1") }
        val received1 = withTimeoutOrNull(500) { connection.incomingFrames.receive() }
        assertNotNull(received1, "The received frame on the first connection should be forwarded")

        connections[0].simulateError("simulated error")
        delay(100) // give time to trigger reconnect coroutine
        assertEquals(2, connections.size, "Should reconnect after error")

        launch { connections[1].simulateTextFrameReceived("test2") }
        val received2 = withTimeoutOrNull(500) { connection.incomingFrames.receive() }
        assertNotNull(received2, "The received frame on the second connection should be forwarded")
        assertTrue(received2 is WebSocketFrame.Text, "The received frame should be a text frame")
        assertEquals(received2.text, "test2", "The message of the forwarded frame should match the received frame")
    }

    @Test
    fun shouldCallOnReconnectWhenReconnected() = runSuspendingTest {
        val connections = mutableListOf<WebSocketConnectionMock>()
        val baseClient = webSocketClientMock {
            WebSocketConnectionMock().also {
                connections.add(it)
            }
        }

        var reconnected = false
        val reconnectingClient = baseClient.withAutoReconnect {
            delayStrategy = FixedDelay(Duration.milliseconds(1))
            afterReconnect {
                reconnected = true
            }
        }
        val connection = reconnectingClient.connect("dummy")
        assertEquals(1, connections.size, "base client should have provided 1 connection")

        launch { connections[0].simulateTextFrameReceived("test1") }
        val received1 = withTimeoutOrNull(500) { connection.incomingFrames.receive() }
        assertNotNull(received1, "The received frame on the first connection should be forwarded")

        assertFalse(reconnected, "afterReconnect callback should not be called after first connection")

        connections[0].simulateError("simulated error")
        delay(100) // give time to trigger reconnect coroutine
        assertEquals(2, connections.size, "Should reconnect after error")
        assertTrue(reconnected, "afterReconnect callback should be called")
    }
}
