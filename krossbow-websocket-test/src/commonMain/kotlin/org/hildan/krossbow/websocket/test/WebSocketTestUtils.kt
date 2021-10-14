package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.WebSocketFrame
import kotlin.test.*

const val DEFAULT_EXPECTED_FRAME_TIMEOUT_MILLIS = 2000L

suspend fun WebSocketClient.connectWithTimeout(
    url: String,
    timeoutMillis: Long = 8000,
) = withTimeoutOrNull(timeoutMillis) { connect(url) }
    ?: fail("Timed out after ${timeoutMillis}ms while connecting to $url")

suspend fun WebSocketConnection.expectTextFrame(
    frameDescription: String,
    timeoutMillis: Long = DEFAULT_EXPECTED_FRAME_TIMEOUT_MILLIS,
) = expectFrame<WebSocketFrame.Text>(frameDescription, timeoutMillis)

suspend fun WebSocketConnection.expectBinaryFrame(
    frameDescription: String,
    timeoutMillis: Long = DEFAULT_EXPECTED_FRAME_TIMEOUT_MILLIS,
) = expectFrame<WebSocketFrame.Binary>(frameDescription, timeoutMillis)

suspend fun WebSocketConnection.expectCloseFrame(
    frameDescription: String = "no more data expected",
    timeoutMillis: Long = DEFAULT_EXPECTED_FRAME_TIMEOUT_MILLIS,
) = expectFrame<WebSocketFrame.Close>(frameDescription, timeoutMillis)

suspend inline fun <reified T : WebSocketFrame> WebSocketConnection.expectFrame(
    frameDescription: String,
    timeoutMillis: Long = DEFAULT_EXPECTED_FRAME_TIMEOUT_MILLIS,
): T {
    val frameType = T::class.simpleName
    val frame = withTimeoutOrNull(timeoutMillis) {
        incomingFrames
            .catch { ex -> fail("Expected $frameType frame ($frameDescription), but the flow failed with $ex") }
            .firstOrNull() ?: fail("Expected $frameType frame ($frameDescription), but the flow was complete")
    }
    assertNotNull(frame, "Timed out while waiting for $frameType frame ($frameDescription)")
    assertIs<T>(frame, "Should have received $frameType frame ($frameDescription), but got $frame")
    return frame
}

suspend fun WebSocketConnection.expectNoMoreFrames(
    eventDescription: String = "end of transmission",
    timeoutMillis: Long = 1000,
) {
    val lastFrames = withTimeoutOrNull(timeoutMillis) { incomingFrames.toList() }
    assertNotNull(lastFrames, "Timed out while waiting for incoming frames flow to end ($eventDescription)")
    assertTrue(lastFrames.isEmpty(), "Frames flow should have completed ($eventDescription), but got ${lastFrames.size} more frame(s): $lastFrames")
}

