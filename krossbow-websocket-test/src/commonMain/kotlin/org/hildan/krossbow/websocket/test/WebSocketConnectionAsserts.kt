package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.WebSocketFrame
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val DEFAULT_EXPECTED_FRAME_TIMEOUT = 5.seconds

suspend fun WebSocketConnection.expectTextFrame(
    frameDescription: String,
    timeout: Duration = DEFAULT_EXPECTED_FRAME_TIMEOUT,
) = expectFrame<WebSocketFrame.Text>(frameDescription, timeout)

suspend fun WebSocketConnection.expectBinaryFrame(
    frameDescription: String,
    timeout: Duration = DEFAULT_EXPECTED_FRAME_TIMEOUT,
) = expectFrame<WebSocketFrame.Binary>(frameDescription, timeout)

suspend fun WebSocketConnection.expectCloseFrame(
    frameDescription: String = "no more data expected",
    timeout: Duration = DEFAULT_EXPECTED_FRAME_TIMEOUT,
) = expectFrame<WebSocketFrame.Close>(frameDescription, timeout)

private suspend inline fun <reified T : WebSocketFrame> WebSocketConnection.expectFrame(
    frameDescription: String,
    timeout: Duration,
): T {
    val frameType = T::class.simpleName
    val frame = withTimeoutOrNull(timeout) {
        incomingFrames
            .catch { ex -> fail("Expected $frameType frame ($frameDescription), but the flow failed with $ex") }
            .firstOrNull() ?: fail("Expected $frameType frame ($frameDescription), but the flow was complete")
    }
    if (frame == null) {
        fail("Timed out while waiting for $frameType frame ($frameDescription)")
    }
    assertIs<T>(frame, "Should have received $frameType frame ($frameDescription), but got $frame")
    return frame
}

suspend fun WebSocketConnection.expectNoMoreFrames(
    eventDescription: String = "end of transmission",
    timeout: Duration = 1.seconds,
) {
    val lastFrames = withTimeoutOrNull(timeout) { incomingFrames.toList() }
    assertNotNull(lastFrames, "Timed out while waiting for incoming frames flow to end ($eventDescription)")
    assertTrue(lastFrames.isEmpty(), "Frames flow should have completed ($eventDescription), but got ${lastFrames.size} more frame(s): $lastFrames")
}

