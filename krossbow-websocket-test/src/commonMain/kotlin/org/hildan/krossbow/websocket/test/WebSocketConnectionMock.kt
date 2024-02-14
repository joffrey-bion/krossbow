package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.channels.Channel
import kotlinx.io.bytestring.*
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerFlowAdapter
import kotlin.test.fail

class WebSocketConnectionMock : WebSocketConnection {

    override val url: String
        get() = "dummy-url"

    private val listener = WebSocketListenerFlowAdapter()

    override val canSend: Boolean
        get() = !closed

    override val incomingFrames = listener.incomingFrames

    private val sentFrames = Channel<WebSocketFrame>()

    private val closeEvent = Channel<CloseEvent>()

    var closed = false

    override suspend fun sendText(frameText: String) {
        sentFrames.send(WebSocketFrame.Text(frameText))
    }

    override suspend fun sendBinary(frameData: ByteString) {
        sentFrames.send(WebSocketFrame.Binary(frameData))
    }

    override suspend fun close(code: Int, reason: String?) {
        closed = true
        closeEvent.send(CloseEvent(code, reason))
    }

    /**
     * Waits for a web socket frame to be sent, unblocking any send call.
     *
     * @returns the web socket frame that was sent to allow further assertions
     */
    suspend fun waitForSentWsFrameAndSimulateCompletion(): WebSocketFrame = sentFrames.receive()

    suspend fun expectClose(): CloseEvent {
        val receiveCatching = closeEvent.receiveCatching()
        if (!receiveCatching.isSuccess) {
            fail("expected web socket close")
        }
        return receiveCatching.getOrThrow()
    }

    fun expectNoClose() {
        if (closeEvent.tryReceive().isSuccess) {
            fail("the web socket close() method should not have been called")
        }
    }

    suspend fun simulateTextFrameReceived(text: String) {
        listener.onTextMessage(text)
    }

    suspend fun simulateBinaryFrameReceived(data: ByteString) {
        listener.onBinaryMessage(data)
    }

    fun simulateError(message: String) {
        listener.onError(Exception(message))
    }

    suspend fun simulateClose(code: Int, reason: String?) {
        listener.onClose(code, reason)
    }
}

data class CloseEvent(val code: Int, val reason: String?)
