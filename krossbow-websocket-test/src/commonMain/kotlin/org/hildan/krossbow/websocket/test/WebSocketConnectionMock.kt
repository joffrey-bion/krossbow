package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import kotlin.test.fail

class WebSocketConnectionMock : WebSocketConnection {

    override val url: String
        get() = "dummy-url"

    private val listener = WebSocketListenerChannelAdapter()

    override val canSend: Boolean
        get() = !closed

    override val incomingFrames = listener.incomingFrames

    private val sentFrames = Channel<WebSocketFrame>()

    private val closeEvent = Channel<Unit>()

    var closed = false

    override suspend fun sendText(frameText: String) {
        sentFrames.send(WebSocketFrame.Text(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        sentFrames.send(WebSocketFrame.Binary(frameData))
    }

    override suspend fun close(code: Int, reason: String?) {
        closed = true
        closeEvent.send(Unit)
    }

    /**
     * Waits for a web socket frame to be sent, unblocking any send call.
     *
     * @returns the parsed stomp frame that was sent to allow further assertions
     */
    suspend fun waitForSentWsFrameAndSimulateCompletion(): WebSocketFrame = sentFrames.receive()

    suspend fun expectClose() {
        if (!closeEvent.receiveCatching().isSuccess) {
            fail("expected web socket close")
        }
    }

    fun expectNoClose() {
        if (closeEvent.tryReceive().isSuccess) {
            fail("the web socket close() method should not have been called")
        }
    }

    suspend fun simulateTextFrameReceived(text: String) {
        listener.onTextMessage(text)
    }

    suspend fun simulateBinaryFrameReceived(data: ByteArray) {
        listener.onBinaryMessage(data)
    }

    fun simulateError(message: String) {
        listener.onError(Exception(message))
    }

    suspend fun simulateClose(code: Int, reason: String?) {
        listener.onClose(code, reason)
    }
}
