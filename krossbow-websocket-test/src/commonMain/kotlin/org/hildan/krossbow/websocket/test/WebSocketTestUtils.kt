package org.hildan.krossbow.websocket.test

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
    val result = withTimeoutOrNull(timeoutMillis) { incomingFrames.receiveCatching() }
    assertNotNull(result, "Timed out while waiting for $frameType frame ($frameDescription)")
    assertFalse(result.isClosed, "Expected $frameType frame ($frameDescription), but the channel was closed")
    assertFalse(result.isFailure, "Expected $frameType frame ($frameDescription), but the channel was failed: ${result.exceptionOrNull()}")

    val frame = result.getOrThrow()
    assertIs<T>(frame, "Should have received $frameType frame ($frameDescription), but got $frame")
    return frame
}

suspend fun WebSocketConnection.expectNoMoreFrames(
    eventDescription: String = "end of transmission",
    timeoutMillis: Long = 1000,
) {
    val result = withTimeoutOrNull(timeoutMillis) { incomingFrames.receiveCatching() }
    assertNotNull(result, "Timed out while waiting for incoming frames channel to be closed ($eventDescription)")
    assertTrue(result.isClosed, "Frames channel should be closed now ($eventDescription), got $result")
}
