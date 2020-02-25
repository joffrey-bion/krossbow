package org.hildan.krossbow.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.StompParser
import org.hildan.krossbow.stomp.frame.encodeToBytes
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.KWebSocketSession
import org.hildan.krossbow.websocket.NoopWebSocketListener
import kotlin.test.assertEquals

class WebSocketSessionMock : KWebSocketSession {

    override var listener: KWebSocketListener = NoopWebSocketListener

    private val sentFrames = Channel<StompFrame>()

    var closed = false

    override suspend fun sendText(frameText: String) {
        sentFrames.send(StompParser.parse(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        sentFrames.send(StompParser.parse(frameData))
    }

    override suspend fun close() {
        closed = true
    }

    suspend fun simulateSendCompletion(): StompFrame = sentFrames.receive()

    suspend fun simulateTextFrameReceived(text: String) {
        listener.onTextMessage(text)
    }

    suspend fun simulateBinaryFrameReceived(data: ByteArray) {
        listener.onBinaryMessage(data)
    }

    suspend fun simulateError(message: String) {
        listener.onError(Exception(message))
    }

    suspend fun simulateClose() {
        listener.onClose()
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

suspend fun WebSocketSessionMock.waitAndAssertSentFrame(expectedCommand: StompCommand) {
    val frame = simulateSendCompletion()
    assertEquals(expectedCommand, frame.command)
}
