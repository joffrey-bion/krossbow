package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
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

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun expectClose() {
        closeEvent.receiveOrNull() ?: fail("expected web socket close")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun expectNoClose() {
        val e = closeEvent.poll()
        if (e != null) {
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
