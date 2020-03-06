package org.hildan.krossbow.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.StompDecoder
import org.hildan.krossbow.stomp.frame.encodeToBytes
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.websocket.WebSocketListener
import org.hildan.krossbow.websocket.WebSocketSession
import org.hildan.krossbow.websocket.NoopWebSocketListener
import kotlin.test.assertEquals

class WebSocketSessionMock : WebSocketSession {

    override var listener: WebSocketListener = NoopWebSocketListener

    private val sentFrames = Channel<StompFrame>()

    var closed = false

    override suspend fun sendText(frameText: String) {
        // decoding the sent frame to check the validity and perform further assertions later
        sentFrames.send(StompDecoder.parse(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        // decoding the sent frame to check the validity and perform further assertions later
        sentFrames.send(StompDecoder.parse(frameData))
    }

    override suspend fun close(code: Int, reason: String?) {
        closed = true
    }

    /**
     * Waits for a web socket frame to be sent, unblocking any send call.
     *
     * @returns the parsed stomp frame that was sent to allow further assertions
     */
    suspend fun waitForSendAndSimulateCompletion(): StompFrame = sentFrames.receive()

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
    simulateTextStompFrameReceived(errorFrame)
    return errorFrame
}

suspend fun WebSocketSessionMock.simulateConnectedFrameReceived() {
    val connectedFrame = StompFrame.Connected(StompConnectedHeaders("1.2"))
    simulateTextStompFrameReceived(connectedFrame)
}

suspend fun WebSocketSessionMock.waitForSendAndSimulateCompletion(expectedCommand: StompCommand): StompFrame {
    val frame = waitForSendAndSimulateCompletion()
    assertEquals(expectedCommand, frame.command, "the next sent frame should be a $expectedCommand STOMP frame")
    return frame
}
