package org.hildan.krossbow.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompDecoder
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.encodeToBytes
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import org.hildan.krossbow.websocket.WebSocketSession
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebSocketSessionMock : WebSocketSession {

    private val listener = WebSocketListenerChannelAdapter()

    override val incomingFrames = listener.incomingFrames

    private val sentFrames = Channel<StompFrame>()

    var closed = false

    override suspend fun sendText(frameText: String) {
        // decoding the sent frame to check the validity and perform further assertions later
        sentFrames.send(StompDecoder.decode(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        // decoding the sent frame to check the validity and perform further assertions later
        sentFrames.send(StompDecoder.decode(frameData))
    }

    override suspend fun close(code: Int, reason: String?) {
        closed = true
    }

    /**
     * Waits for a web socket frame to be sent, unblocking any send call.
     *
     * @returns the parsed stomp frame that was sent to allow further assertions
     */
    suspend fun waitForSentFrameAndSimulateCompletion(): StompFrame = sentFrames.receive()

    suspend fun simulateTextFrameReceived(text: String) {
        listener.onTextMessage(text)
    }

    suspend fun simulateBinaryFrameReceived(data: ByteArray) {
        listener.onBinaryMessage(data)
    }

    suspend fun simulateError(message: String) {
        listener.onError(Exception(message))
    }

    suspend fun simulateClose(code: Int, reason: String?) {
        listener.onClose(code, reason)
    }
}

suspend fun WebSocketSessionMock.simulateTextStompFrameReceived(frame: StompFrame) {
    simulateTextFrameReceived(frame.encodeToText())
}

suspend fun WebSocketSessionMock.simulateBinaryStompFrameReceived(frame: StompFrame) {
    simulateBinaryFrameReceived(frame.encodeToBytes())
}

suspend fun WebSocketSessionMock.simulateErrorFrameReceived(errorMessage: String): StompFrame.Error {
    val errorFrame = StompFrame.Error(StompErrorHeaders(errorMessage), null)
    val result = runCatching {
        simulateTextStompFrameReceived(errorFrame)
    }
    assertTrue(
        result.isSuccess,
        "Calling the listener with an error frame is the responsibility of the web " +
                "socket implementation, and is done from a thread that we don't control, so " +
                "we don't want that to fail."
    )
    return errorFrame
}

suspend fun WebSocketSessionMock.simulateMessageFrameReceived(
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

suspend fun WebSocketSessionMock.simulateConnectedFrameReceived(
    connectedHeaders: StompConnectedHeaders = StompConnectedHeaders()
) {
    val connectedFrame = StompFrame.Connected(connectedHeaders)
    simulateTextStompFrameReceived(connectedFrame)
}

suspend fun WebSocketSessionMock.waitForSendAndSimulateCompletion(expectedCommand: StompCommand): StompFrame {
    val frame = waitForSentFrameAndSimulateCompletion()
    assertEquals(expectedCommand, frame.command, "the next sent frame should be a $expectedCommand STOMP frame")
    return frame
}

suspend fun WebSocketSessionMock.waitForSubscribeAndSimulateCompletion(): StompFrame.Subscribe {
    val frame = waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
    assertTrue(frame is StompFrame.Subscribe)
    return frame
}
