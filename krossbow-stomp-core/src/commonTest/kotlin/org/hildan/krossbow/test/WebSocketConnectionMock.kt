package org.hildan.krossbow.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import org.hildan.krossbow.stomp.frame.*
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import org.hildan.krossbow.websocket.WebSocketConnection
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class WebSocketConnectionMock : WebSocketConnection {

    override val url: String
        get() = "dummy-url"

    private val listener = WebSocketListenerChannelAdapter()

    override val canSend: Boolean
        get() = !closed

    override val incomingFrames = listener.incomingFrames

    private val sentFrames = Channel<StompFrame>()

    private val closeEvent = Channel<Unit>()

    var closed = false

    override suspend fun sendText(frameText: String) {
        // decoding the sent frame to check the validity and perform further assertions later
        sendStompFrame(StompDecoder.decode(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        // decoding the sent frame to check the validity and perform further assertions later
        sendStompFrame(StompDecoder.decode(frameData))
    }

    private suspend fun sendStompFrame(stompFrame: StompFrame) {
        sentFrames.send(stompFrame)
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
    suspend fun waitForSentFrameAndSimulateCompletion(): StompFrame = sentFrames.receive()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun expectClose() {
        val e = closeEvent.receiveOrNull()
        if (e == null) {
            fail("Expected web socket close")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun expectNoClose() {
        val e = closeEvent.poll()
        if (e != null) {
            fail("the web socket close() method should not be called")
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

suspend fun WebSocketConnectionMock.simulateTextStompFrameReceived(frame: StompFrame) {
    simulateTextFrameReceived(frame.encodeToText())
}

suspend fun WebSocketConnectionMock.simulateBinaryStompFrameReceived(frame: StompFrame) {
    simulateBinaryFrameReceived(frame.encodeToBytes())
}

suspend fun WebSocketConnectionMock.simulateErrorFrameReceived(errorMessage: String): StompFrame.Error {
    val errorFrame = StompFrame.Error(StompErrorHeaders(errorMessage), null)
    val result = runCatching {
        simulateTextStompFrameReceived(errorFrame)
    }
    assertTrue(
        result.isSuccess,
        "Calling the listener with an error frame is the responsibility of the web socket implementation, and " +
            "is done from a thread that we don't control, so we don't want that to fail."
    )
    return errorFrame
}

suspend fun WebSocketConnectionMock.simulateMessageFrameReceived(
    subId: String,
    body: String?,
    destination: String = "/destination",
    messageId: String = "42"
): StompFrame.Message {
    val headers = StompMessageHeaders(destination, messageId, subId)
    val frame = StompFrame.Message(headers, body?.let { FrameBody.Text(it) })
    simulateTextStompFrameReceived(frame)
    return frame
}

suspend fun WebSocketConnectionMock.simulateConnectedFrameReceived(
    connectedHeaders: StompConnectedHeaders = StompConnectedHeaders()
) {
    val connectedFrame = StompFrame.Connected(connectedHeaders)
    simulateTextStompFrameReceived(connectedFrame)
}

suspend fun WebSocketConnectionMock.simulateReceiptFrameReceived(receiptId: String) {
    simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(receiptId)))
}

suspend fun WebSocketConnectionMock.waitForSendAndSimulateCompletion(expectedCommand: StompCommand): StompFrame {
    val frame = waitForSentFrameAndSimulateCompletion()
    assertEquals(expectedCommand, frame.command, "The next sent frame should be a $expectedCommand STOMP frame.")
    return frame
}

suspend fun WebSocketConnectionMock.waitForSubscribeAndSimulateCompletion(): StompFrame.Subscribe {
    val frame = waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
    assertTrue(frame is StompFrame.Subscribe)
    return frame
}

suspend fun WebSocketConnectionMock.waitForUnsubscribeAndSimulateCompletion(expectedSubId: String): StompFrame.Unsubscribe {
    val frame = waitForSendAndSimulateCompletion(StompCommand.UNSUBSCRIBE)
    assertTrue(frame is StompFrame.Unsubscribe)
    assertEquals(expectedSubId, frame.headers.id)
    return frame
}

suspend fun WebSocketConnectionMock.waitForDisconnectAndSimulateCompletion(): StompFrame.Disconnect {
    val frame = waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
    assertTrue(frame is StompFrame.Disconnect)
    return frame
}
